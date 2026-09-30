package com.flxrs.dankchat.data.api.seventv

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.recoverNotFoundWith
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteSetDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVGqlResponse
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintsDataDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUserDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUsersConnectionDataDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import com.flxrs.dankchat.data.twitch.paint.toSevenTVPaint
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class SevenTVApiClient(
    private val sevenTVApi: SevenTVApi,
    private val json: Json,
) {
    suspend fun getSevenTVChannelEmotes(channelId: UserId): Result<SevenTVUserDto?> = runCatching {
        sevenTVApi
            .getChannelEmotes(channelId)
            .throwApiErrorOnFailure(json)
            .body<SevenTVUserDto>()
    }.recoverNotFoundWith(default = null)

    suspend fun getSevenTVEmoteSet(emoteSetId: String): Result<SevenTVEmoteSetDto> = runCatching {
        sevenTVApi
            .getEmoteSet(emoteSetId)
            .throwApiErrorOnFailure(json)
            .body()
    }

    suspend fun getSevenTVGlobalEmotes(): Result<List<SevenTVEmoteDto>> = runCatching {
        sevenTVApi
            .getGlobalEmotes()
            .throwApiErrorOnFailure(json)
            .body<SevenTVEmoteSetDto>()
            .emotes
            .orEmpty()
    }

    suspend fun getSevenTVPaints(): Result<List<SevenTVPaint>> = runCatching {
        val response = sevenTVApi
            .getPaints()
            .throwApiErrorOnFailure(json)
            .body<SevenTVGqlResponse<SevenTVPaintsDataDto>>()

        response.data
            ?.paints
            ?.paints
            .orEmpty()
            .map { it.toSevenTVPaint() }
    }

    suspend fun getSevenTVUsersPaints(userIds: List<UserId>): Result<Map<UserId, String?>> = runCatching {
        if (userIds.isEmpty()) return@runCatching emptyMap()
        val response = sevenTVApi
            .getUsersPaints(userIds)
            .throwApiErrorOnFailure(json)
            .body<SevenTVGqlResponse<SevenTVUsersConnectionDataDto>>()

        val usersMap = response.data?.users.orEmpty()
        val result = mutableMapOf<UserId, String?>()
        userIds.forEachIndexed { index, userId ->
            val userDto = usersMap["u$index"]
            val paintId = userDto?.style?.activePaintId
            result[userId] = paintId
        }
        result
    }
}
