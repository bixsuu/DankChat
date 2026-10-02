package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.SevenTVPaintsRepository
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.badge.BadgeSet
import com.flxrs.dankchat.data.twitch.badge.BadgeVersion
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmoteType
import com.flxrs.dankchat.data.twitch.message.EmoteWithPositions
import com.flxrs.dankchat.data.twitch.message.Message
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.data.api.ffz.dto.FFZEmoteDto
import com.flxrs.dankchat.data.api.ffz.dto.FFZEmoteSetDto
import com.flxrs.dankchat.data.api.ffz.dto.FFZGlobalDto
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.hasAnimatedEffects
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isAppear
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isGrowX
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isLeave
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

@ExtendWith(MockKExtension::class)
internal class EmoteRepositoryTest {
    @MockK
    lateinit var helixApiClient: HelixApiClient

    @MockK
    lateinit var chatSettings: ChatSettingsDataStore

    @MockK
    lateinit var channelRepository: ChannelRepository

    @MockK
    lateinit var sevenTVPaintsRepository: SevenTVPaintsRepository

    @MockK
    lateinit var dispatchersProvider: DispatchersProvider

    @InjectMockKs
    lateinit var emoteRepository: EmoteRepository

    // --- parseTwitchEmotes tests ---

    @Test
    fun `emote positions are correct for regular message without emoji`() {
        // "hello Kappa world" — Kappa at position 6..10
        val message = "hello Kappa world"
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(6..10)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = emptyList(),
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = 0,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 6..11, actual = result.single().position)
    }

    @Test
    fun `emotes with out of bounds positions are skipped`() {
        val message = "hello Kappa"
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(999..1005)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = emptyList(),
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = 0,
        )
        assertEquals(expected = emptyList(), actual = result)
    }

    @Test
    fun `emotes with inverted positions are skipped`() {
        val message = "hello Kappa"
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(5..2)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = emptyList(),
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = 0,
        )
        assertEquals(expected = emptyList(), actual = result)
    }

    @Test
    fun `emote positions are correct with removed duplicate whitespace before an emoji`() {
        // Original: "a   😂 Kappa" — Kappa at codepoints 6..10; whitespace dedup removes
        // codepoints 2 and 3 and shifts the emoji to codepoint 2 of "a 😂 Kappa"
        val message = "a 😂 Kappa"
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(6..10)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = listOf(2),
            appendedSpaces = emptyList(),
            removedSpaces = listOf(2, 3),
            replyMentionOffset = 0,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 5..10, actual = result.single().position)
    }

    @Test
    fun `emote positions are correct for reply message without emoji`() {
        // Original: "@someuser hello Kappa world" — Kappa at Twitch position 16..20
        // Stripped: "hello Kappa world" — replyMentionOffset = 10 ("@someuser " = 10 chars)
        val message = "hello Kappa world"
        val replyOffset = 10
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(16..20)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = emptyList(),
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = replyOffset,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 6..11, actual = result.single().position)
    }

    @Test
    fun `emote positions are correct with flag emoji before emote`() {
        // "nice play 🇩🇪 Kappa" — 🇩🇪 = U+1F1E9 U+1F1EA (two supplementary codepoints)
        // Twitch codepoint positions: n=0..y=8, ' '=9, 🇩=10, 🇪=11, ' '=12, K=13..a=17
        // supplementaryCodePointPositions: [10, 11]
        // unicodeExtra = 2 → fixedStart = 13 + 2 = 15
        // Kotlin string: "nice play " = 0..9, 🇩 = 10-11, 🇪 = 12-13, ' ' = 14, K = 15
        val message = "nice play 🇩🇪 Kappa"
        val supplementary = listOf(10, 11)
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(13..17)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = supplementary,
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = 0,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 15..20, actual = result.single().position)
    }

    @Test
    fun `emote positions are correct with skin tone emoji before emote`() {
        // "GG 👍🏽 Kappa" — 👍🏽 = U+1F44D U+1F3FD (two supplementary codepoints)
        // Twitch codepoints: G=0, G=1, ' '=2, 👍=3, 🏽=4, ' '=5, K=6..a=10
        // supplementaryCodePointPositions: [3, 4]
        // unicodeExtra = 2 → fixedStart = 6 + 2 = 8
        // Kotlin string: G=0, G=1, ' '=2, 👍=3-4, 🏽=5-6, ' '=7, K=8
        val message = "GG 👍🏽 Kappa"
        val supplementary = listOf(3, 4)
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(6..10)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = supplementary,
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = 0,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 8..13, actual = result.single().position)
    }

    @Test
    fun `reply with flag emoji before emote adjusts positions correctly`() {
        // Original: "@treejadey nice play 🇩🇪 Kappa"
        // "@treejadey " = 12 codepoints, stripped: "nice play 🇩🇪 Kappa"
        // Twitch Kappa position: 25..29 (13 + 12), replyMentionOffset = 12
        // adjustedFirst = 25 - 12 = 13, unicodeExtra = countLessThan([10, 11], 13) = 2
        // fixedStart = 13 + 2 = 15 → Kotlin index 15 = 'K' ✓
        val message = "nice play 🇩🇪 Kappa"
        val replyOffset = 12
        val supplementary = listOf(10, 11)
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(25..29)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = supplementary,
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = replyOffset,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 15..20, actual = result.single().position)
    }

    @Test
    fun `reply with skin tone emoji before emote adjusts positions correctly`() {
        // Original: "@flex3rs GG 👍🏽 Kappa"
        // "@flex3rs " = 9 codepoints, stripped: "GG 👍🏽 Kappa"
        // Twitch Kappa position: 15..19 (6 + 9), replyMentionOffset = 9
        // adjustedFirst = 15 - 9 = 6, unicodeExtra = countLessThan([3, 4], 6) = 2
        // fixedStart = 6 + 2 = 8 → Kotlin index 8 = 'K' ✓
        val message = "GG 👍🏽 Kappa"
        val replyOffset = 9
        val supplementary = listOf(3, 4)
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(15..19)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = supplementary,
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = replyOffset,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 8..13, actual = result.single().position)
    }

    @Test
    fun `emote positions are correct with emoji after emote`() {
        // "Kappa 🇩🇪 nice" — emoji is after emote, should not affect emote position
        // Twitch codepoints: K=0..a=4, ' '=5, 🇩=6, 🇪=7, ' '=8, n=9..e=12
        // Kappa at 0..4, supplementary at [6, 7] — both after emote
        // unicodeExtra = countLessThan([6, 7], 0) = 0
        val message = "Kappa 🇩🇪 nice"
        val supplementary = listOf(6, 7)
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(0..4)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = supplementary,
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = 0,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 0..5, actual = result.single().position)
    }

    @Test
    fun `reply with emote between emojis`() {
        // Original: "@user 👍🏽 Kappa ⚡" — "@user " = 6, stripped: "👍🏽 Kappa ⚡"
        // Twitch codepoints (full): 👍=6, 🏽=7, ' '=8, K=9..a=13, ' '=14, ⚡=15
        // Kappa at 9..13, replyMentionOffset = 6
        // Stripped supplementary: 👍 at 0, 🏽 at 1, ⚡ at 8
        // adjustedFirst = 9 - 6 = 3, unicodeExtra = countLessThan([0, 1, 8], 3) = 2
        // fixedStart = 3 + 2 = 5
        // Kotlin string "👍🏽 Kappa ⚡": 👍=0-1, 🏽=2-3, ' '=4, K=5
        val message = "👍🏽 Kappa ⚡"
        val replyOffset = 6
        val supplementary = listOf(0, 1, 8)
        val emotes = listOf(EmoteWithPositions(id = "25", positions = listOf(9..13)))
        val result = emoteRepository.parseTwitchEmotes(
            emotesWithPositions = emotes,
            message = message,
            supplementaryCodePointPositions = supplementary,
            appendedSpaces = emptyList(),
            removedSpaces = emptyList(),
            replyMentionOffset = replyOffset,
        )
        assertEquals(expected = "Kappa", actual = result.single().code)
        assertEquals(expected = 5..10, actual = result.single().position)
    }

    @Test
    fun `overlay emotes are not moved if regular text is in-between`() {
        val message = "FeelsDankMan asd cvHazmat RainTime"
        val emotes =
            listOf(
                ChatMessageEmote(position = 0..12, url = "asd", id = "1", code = "FeelsDankMan", scale = 1, type = ChatMessageEmoteType.TwitchEmote),
                ChatMessageEmote(position = 17..25, url = "asd", id = "1", code = "cvHazmat", scale = 1, type = ChatMessageEmoteType.TwitchEmote, isOverlayEmote = true),
                ChatMessageEmote(position = 26..34, url = "asd", id = "1", code = "cvHazmat", scale = 1, type = ChatMessageEmoteType.TwitchEmote, isOverlayEmote = true),
            )
        val (resultMessage, resultEmotes) = emoteRepository.adjustOverlayEmotes(message, emotes)

        assertEquals(expected = message, actual = resultMessage)
        assertEquals(expected = emotes, actual = resultEmotes)
    }

    @Test
    fun `overlay emotes are moved if no regular text is in-between`() {
        val message = "FeelsDankMan cvHazmat RainTime"
        val emotes =
            listOf(
                ChatMessageEmote(position = 0..12, url = "asd", id = "1", code = "FeelsDankMan", scale = 1, type = ChatMessageEmoteType.TwitchEmote),
                ChatMessageEmote(position = 13..21, url = "asd", id = "1", code = "cvHazmat", scale = 1, type = ChatMessageEmoteType.TwitchEmote, isOverlayEmote = true),
                ChatMessageEmote(position = 22..30, url = "asd", id = "1", code = "cvHazmat", scale = 1, type = ChatMessageEmoteType.TwitchEmote, isOverlayEmote = true),
            )
        val expectedMessage = "FeelsDankMan " // KKona
        val expectedEmotes =
            listOf(
                ChatMessageEmote(position = 0..12, url = "asd", id = "1", code = "FeelsDankMan", scale = 1, type = ChatMessageEmoteType.TwitchEmote),
                ChatMessageEmote(position = 0..12, url = "asd", id = "1", code = "cvHazmat", scale = 1, type = ChatMessageEmoteType.TwitchEmote, isOverlayEmote = true),
                ChatMessageEmote(position = 0..12, url = "asd", id = "1", code = "cvHazmat", scale = 1, type = ChatMessageEmoteType.TwitchEmote, isOverlayEmote = true),
            )

        val (resultMessage, resultEmotes) = emoteRepository.adjustOverlayEmotes(message, emotes)

        assertEquals(expected = expectedMessage, actual = resultMessage)
        assertEquals(expected = expectedEmotes, actual = resultEmotes)
    }

    @Test
    fun `adjustOverlayEmotes moves FFZ overlay modifier to preceding emote`() {
        val message = "Kappa StabZ"
        val emotes =
            listOf(
                ChatMessageEmote(position = 0..5, url = "url_kappa", id = "25", code = "Kappa", scale = 1, type = ChatMessageEmoteType.TwitchEmote),
                ChatMessageEmote(position = 6..11, url = "url_stabz", id = "100", code = "StabZ", scale = 1, type = ChatMessageEmoteType.GlobalFFZEmote(creator = null), isOverlayEmote = true, modifierFlags = 0),
            )
        val (resultMessage, resultEmotes) = emoteRepository.adjustOverlayEmotes(message, emotes)

        assertEquals(expected = "Kappa ", actual = resultMessage)
        assertEquals(expected = 0..5, actual = resultEmotes[0].position)
        assertEquals(expected = 0..5, actual = resultEmotes[1].position)
    }

    @Test
    fun `adjustOverlayEmotes moves FFZ effect modifier to preceding emote`() {
        val message = "Kappa ffzX"
        val emotes =
            listOf(
                ChatMessageEmote(position = 0..5, url = "url_kappa", id = "25", code = "Kappa", scale = 1, type = ChatMessageEmoteType.TwitchEmote),
                ChatMessageEmote(
                    position = 6..10,
                    url = "url_ffzx",
                    id = "101",
                    code = "ffzX",
                    scale = 1,
                    type = ChatMessageEmoteType.GlobalFFZEmote(creator = null),
                    isOverlayEmote = true,
                    modifierFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.FLIP_X,
                ),
            )
        val (resultMessage, resultEmotes) = emoteRepository.adjustOverlayEmotes(message, emotes)

        assertEquals(expected = "Kappa ", actual = resultMessage)
        assertEquals(expected = 0..5, actual = resultEmotes[0].position)
        assertEquals(expected = 0..5, actual = resultEmotes[1].position)
        assertEquals(expected = FFZModifierFlags.HIDDEN or FFZModifierFlags.FLIP_X, actual = resultEmotes[1].modifierFlags)
    }

    @Test
    fun `adjustOverlayEmotes chains multiple FFZ modifiers to preceding emote`() {
        val message = "Kappa StabZ ffzX"
        val emotes =
            listOf(
                ChatMessageEmote(position = 0..5, url = "url_kappa", id = "25", code = "Kappa", scale = 1, type = ChatMessageEmoteType.TwitchEmote),
                ChatMessageEmote(position = 6..11, url = "url_stabz", id = "100", code = "StabZ", scale = 1, type = ChatMessageEmoteType.GlobalFFZEmote(creator = null), isOverlayEmote = true),
                ChatMessageEmote(
                    position = 12..16,
                    url = "url_ffzx",
                    id = "101",
                    code = "ffzX",
                    scale = 1,
                    type = ChatMessageEmoteType.GlobalFFZEmote(creator = null),
                    isOverlayEmote = true,
                    modifierFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.FLIP_X,
                ),
            )
        val (resultMessage, resultEmotes) = emoteRepository.adjustOverlayEmotes(message, emotes)

        assertEquals(expected = "Kappa ", actual = resultMessage)
        assertEquals(expected = 0..5, actual = resultEmotes[0].position)
        assertEquals(expected = 0..5, actual = resultEmotes[1].position)
        assertEquals(expected = 0..5, actual = resultEmotes[2].position)
    }

    @Test
    fun `setFFZGlobalEmotes parses regular, overlay modifier, and Subwoofer effect modifier emotes`() = runBlocking {
        every { dispatchersProvider.default } returns Dispatchers.Unconfined

        val defaultSetId = "3"
        val subwooferSetId = "1532818"
        val ffzGlobalDto =
            FFZGlobalDto(
                defaultSets = listOf(kotlinx.serialization.json.JsonPrimitive(defaultSetId)),
                sets =
                    mapOf(
                        defaultSetId to
                            FFZEmoteSetDto(
                                emotes =
                                    listOf(
                                        FFZEmoteDto(
                                            id = 1,
                                            name = "CatBag",
                                            urls = mapOf("1" to "https://cdn.ffz.com/1"),
                                            animated = null,
                                            owner = null,
                                            isModifier = false,
                                            modifierFlags = 0,
                                        ),
                                        FFZEmoteDto(
                                            id = 2,
                                            name = "StabZ",
                                            urls = mapOf("1" to "https://cdn.ffz.com/2"),
                                            animated = null,
                                            owner = null,
                                            isModifier = true,
                                            modifierFlags = 0,
                                        ),
                                    ),
                            ),
                        subwooferSetId to
                            FFZEmoteSetDto(
                                emotes =
                                    listOf(
                                        FFZEmoteDto(
                                            id = 3,
                                            name = "ffzHyper",
                                            urls = mapOf("1" to "https://cdn.ffz.com/3"),
                                            animated = null,
                                            owner = null,
                                            isModifier = true,
                                            modifierFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.HYPER_RED,
                                        ),
                                        FFZEmoteDto(
                                            id = 4,
                                            name = "NonModifierSubwooferEmote",
                                            urls = mapOf("1" to "https://cdn.ffz.com/4"),
                                            animated = null,
                                            owner = null,
                                            isModifier = false,
                                            modifierFlags = 0,
                                        ),
                                    ),
                            ),
                    ),
            )

        emoteRepository.setFFZGlobalEmotes(ffzGlobalDto)

        val parsed =
            emoteRepository.parse3rdPartyEmotes(
                message = "CatBag StabZ ffzHyper NonModifierSubwooferEmote",
                channel = "test".toUserName(),
            )
        assertEquals(3, parsed.size)

        val catBag = parsed.first { it.code == "CatBag" }
        assertEquals(false, catBag.isOverlayEmote)
        assertEquals(0, catBag.modifierFlags)

        val stabZ = parsed.first { it.code == "StabZ" }
        assertEquals(true, stabZ.isOverlayEmote)
        assertEquals(0, stabZ.modifierFlags)

        val ffzHyper = parsed.first { it.code == "ffzHyper" }
        assertEquals(true, ffzHyper.isOverlayEmote)
        assertEquals(FFZModifierFlags.HIDDEN or FFZModifierFlags.HYPER_RED, ffzHyper.modifierFlags)
    }

    @Test
    fun `parseEmotesAndBadges on Twitch emote with ffzX modifier`() = runBlocking {
        every { dispatchersProvider.default } returns Dispatchers.Unconfined
        val defaultSetId = "3"
        val dto =
            FFZGlobalDto(
                defaultSets = listOf(kotlinx.serialization.json.JsonPrimitive(defaultSetId)),
                sets =
                    mapOf(
                        defaultSetId to
                            FFZEmoteSetDto(
                                emotes =
                                    listOf(
                                        FFZEmoteDto(
                                            id = 720508,
                                            name = "ffzX",
                                            urls = mapOf("1" to "https://cdn.ffz.com/720508/1"),
                                            animated = null,
                                            owner = null,
                                            isModifier = true,
                                            modifierFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.FLIP_X,
                                        ),
                                    ),
                            ),
                    ),
            )
        emoteRepository.setFFZGlobalEmotes(dto)

        // Raw Twitch message for "Kappa ffzX" where Kappa is Twitch emote 25 at 0-4
        val raw = "@badges=;color=#F1C40F;display-name=test;emotes=25:0-4;id=1;room-id=1;tmi-sent-ts=1;user-id=1 :test!test@test.tmi.twitch.tv PRIVMSG #test :Kappa ffzX"
        val message = assertIs<PrivMessage>(Message.parse(IrcMessage.parse(raw)) { null })
        val parsed = assertIs<PrivMessage>(emoteRepository.parseEmotesAndBadges(message))

        assertEquals(2, parsed.emotes.size)
        assertEquals(parsed.emotes[0].position, parsed.emotes[1].position)
        assertEquals(FFZModifierFlags.HIDDEN or FFZModifierFlags.FLIP_X, parsed.emotes[1].modifierFlags)
    }

    @Test
    fun `FFZ modifier flags identify arrive, leave, and wide`() {
        val arriveFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.APPEAR
        val leaveFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.LEAVE
        val wideFlags = FFZModifierFlags.HIDDEN or FFZModifierFlags.GROW_X

        assertEquals(true, arriveFlags.isAppear)
        assertEquals(true, arriveFlags.hasAnimatedEffects)

        assertEquals(true, leaveFlags.isLeave)
        assertEquals(true, leaveFlags.hasAnimatedEffects)

        assertEquals(true, wideFlags.isGrowX)
        assertEquals(false, wideFlags.hasAnimatedEffects)
    }

    // --- badge parsing tests ---

    @Test
    fun `campaign badges resolve from channel badge sets without a global counterpart`() = runBlocking {
        val campaignSetId = "campaign-38949074-1e80f2d1-4993-45e4-8c07-14699b3f4f02-mw"
        emoteRepository.setChannelBadges(
            channel = "icdb".toUserName(),
            badges =
                mapOf(
                    campaignSetId to
                        BadgeSet(
                            id = campaignSetId,
                            versions =
                                mapOf(
                                    "1" to BadgeVersion(id = "1", title = "FeelsDankMan", imageUrlLow = "1x", imageUrlMedium = "2x", imageUrlHigh = "4x"),
                                ),
                        ),
                ),
        )

        val raw =
            "@badge-info=;badges=moderator/1,$campaignSetId/1;color=#F1C40F;display-name=sunred_;emotes=;first-msg=0;flags=;id=4e0f1b81-0c65-42cf-b52b-21386d288e8a;mod=1;returning-chatter=0;room-id=38949074;subscriber=0;tmi-sent-ts=1786793453714;turbo=0;user-id=99308836;user-type=mod :sunred_!sunred_@sunred_.tmi.twitch.tv PRIVMSG #icdb :-tags"
        val message = assertIs<PrivMessage>(Message.parse(IrcMessage.parse(raw)) { null })

        val parsed = assertIs<PrivMessage>(emoteRepository.parseEmotesAndBadges(message))

        val campaignBadge = assertIs<Badge.ChannelBadge>(parsed.badges.single())
        assertEquals(expected = "FeelsDankMan", actual = campaignBadge.title)
        assertEquals(expected = "4x", actual = campaignBadge.url)
    }
}
