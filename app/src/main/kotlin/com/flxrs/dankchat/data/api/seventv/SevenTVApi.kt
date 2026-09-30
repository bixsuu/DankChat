package com.flxrs.dankchat.data.api.seventv

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVGqlQuery
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SevenTVApi(
    private val ktorClient: HttpClient,
) {
    suspend fun getChannelEmotes(channelId: UserId) = ktorClient.get("users/twitch/$channelId")

    suspend fun getEmoteSet(emoteSetId: String) = ktorClient.get("emote-sets/$emoteSetId")

    suspend fun getGlobalEmotes() = ktorClient.get("emote-sets/global")

    suspend fun getPaints(): HttpResponse = ktorClient.post(SEVENTV_V4_GQL_URL) {
        contentType(ContentType.Application.Json)
        setBody(SevenTVGqlQuery(query = PAINTS_QUERY))
    }

    suspend fun getUsersPaints(userIds: List<UserId>): HttpResponse {
        val userQueries = userIds
            .mapIndexed { index, userId ->
                "u$index: userByConnection(platform: TWITCH, platformId: \"$userId\") { id style { activePaintId } }"
            }.joinToString(" ")
        val query = "query { users { $userQueries } }"
        return ktorClient.post(SEVENTV_V4_GQL_URL) {
            contentType(ContentType.Application.Json)
            setBody(SevenTVGqlQuery(query = query))
        }
    }

    companion object {
        const val SEVENTV_V4_GQL_URL = "https://api.7tv.app/v4/gql"

        private val PAINTS_QUERY =
            """
            query {
              paints {
                paints {
                  id
                  name
                  data {
                    layers {
                      id
                      opacity
                      ty {
                        __typename
                        ... on PaintLayerTypeSingleColor {
                          color { hex }
                        }
                        ... on PaintLayerTypeLinearGradient {
                          angle
                          repeating
                          stops { at color { hex } }
                        }
                        ... on PaintLayerTypeRadialGradient {
                          shape
                          repeating
                          stops { at color { hex } }
                        }
                        ... on PaintLayerTypeImage {
                          images { url mime scale width height }
                        }
                      }
                    }
                    shadows {
                      color { hex }
                      offsetX
                      offsetY
                      blur
                    }
                  }
                }
              }
            }
            """.trimIndent()
    }
}
