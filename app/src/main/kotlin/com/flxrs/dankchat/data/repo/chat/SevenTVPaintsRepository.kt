package com.flxrs.dankchat.data.repo.chat

import androidx.collection.LruCache
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.seventv.SevenTVApiClient
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Single
class SevenTVPaintsRepository(
    private val sevenTVApiClient: SevenTVApiClient,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val paints = ConcurrentHashMap<String, SevenTVPaint>()
    private val cacheLock = Any()
    private val userPaintsCache = LruCache<UserId, CachedUserPaint>(USER_PAINT_CACHE_SIZE)
    private val pendingUsers = ConcurrentHashMap.newKeySet<UserId>()
    private val scheduledBatch = AtomicBoolean(false)

    private val _paintsVersion = MutableStateFlow(0L)
    val paintsVersion: StateFlow<Long> = _paintsVersion.asStateFlow()

    suspend fun loadPaints(): Result<Unit> = withContext(dispatchersProvider.io) {
        val showPaints = chatSettingsDataStore.settings.first().showSevenTvPaints
        if (!showPaints) {
            return@withContext Result.success(Unit)
        }

        sevenTVApiClient
            .getSevenTVPaints()
            .map { list ->
                paints.putAll(list.associateBy { it.id })
                _paintsVersion.update { it + 1 }
                logger.info { "Loaded ${list.size} 7TV paints" }
            }.onFailure {
                logger.error(it) { "Failed to load 7TV paints" }
            }.map { }
    }

    fun getPaintForUser(userId: UserId): SevenTVPaint? {
        if (userId.value.isEmpty()) return null

        val cached = synchronized(cacheLock) {
            userPaintsCache.get(userId)
        }

        if (cached != null) {
            val paintId = cached.paintId ?: return null
            return paints[paintId]
        }

        enqueueUserForPaint(userId)
        return null
    }

    fun enqueueUserForPaint(userId: UserId) {
        if (userId.value.isEmpty()) return
        val alreadyCached = synchronized(cacheLock) {
            userPaintsCache.get(userId) != null
        }
        if (alreadyCached) return

        pendingUsers.add(userId)
        if (scheduledBatch.compareAndSet(false, true)) {
            scope.launch {
                delay(BATCH_DEBOUNCE_INTERVAL)
                scheduledBatch.set(false)
                processPendingUsers()
            }
        }
    }

    fun seedUserPaint(
        userId: UserId,
        paintId: String?,
    ) {
        if (userId.value.isEmpty()) return
        synchronized(cacheLock) {
            userPaintsCache.put(userId, CachedUserPaint(paintId))
        }
        _paintsVersion.update { it + 1 }
    }

    fun getPaintById(paintId: String): SevenTVPaint? = paints[paintId]

    private suspend fun processPendingUsers() {
        withContext(dispatchersProvider.io) {
            val showPaints = chatSettingsDataStore.current().showSevenTvPaints
            if (!showPaints) {
                pendingUsers.clear()
                return@withContext
            }

            if (paints.isEmpty()) {
                loadPaints()
            }

            val toFetch = mutableListOf<UserId>()
            val iterator = pendingUsers.iterator()
            while (iterator.hasNext() && toFetch.size < MAX_USERS_PER_REQUEST) {
                toFetch.add(iterator.next())
                iterator.remove()
            }

            if (toFetch.isEmpty()) return@withContext

            val result = sevenTVApiClient.getSevenTVUsersPaints(toFetch).getOrNull()
            var updated = false
            if (result != null) {
                synchronized(cacheLock) {
                    for (userId in toFetch) {
                        val paintId = result[userId]
                        userPaintsCache.put(userId, CachedUserPaint(paintId))
                        if (paintId != null && paints.containsKey(paintId)) {
                            updated = true
                        }
                    }
                }
            }

            if (pendingUsers.isNotEmpty()) {
                if (scheduledBatch.compareAndSet(false, true)) {
                    scope.launch {
                        delay(BATCH_DEBOUNCE_INTERVAL)
                        scheduledBatch.set(false)
                        processPendingUsers()
                    }
                }
            }

            if (updated) {
                _paintsVersion.update { it + 1 }
            }
        }
    }

    data class CachedUserPaint(
        val paintId: String?,
    )

    companion object {
        private val logger = KotlinLogging.logger("SevenTVPaintsRepository")
        private const val USER_PAINT_CACHE_SIZE = 5000
        private const val MAX_USERS_PER_REQUEST = 50
        private val BATCH_DEBOUNCE_INTERVAL: Duration = 300.milliseconds
    }
}
