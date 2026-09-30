package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Keep
@Serializable
data class SevenTVGqlQuery(
    val query: String,
    val variables: JsonObject? = null,
)

@Keep
@Serializable
data class SevenTVGqlResponse<T>(
    val data: T? = null,
    val errors: List<SevenTVGqlErrorDto>? = null,
)

@Keep
@Serializable
data class SevenTVGqlErrorDto(
    val message: String? = null,
)

@Keep
@Serializable
data class SevenTVPaintsDataDto(
    val paints: SevenTVPaintsContainerDto? = null,
)

@Keep
@Serializable
data class SevenTVPaintsContainerDto(
    val paints: List<SevenTVPaintDto> = emptyList(),
)

@Keep
@Serializable
data class SevenTVPaintDto(
    val id: String,
    val name: String,
    val data: SevenTVPaintDataDto,
)

@Keep
@Serializable
data class SevenTVPaintDataDto(
    val layers: List<SevenTVPaintLayerDto> = emptyList(),
    val shadows: List<SevenTVPaintShadowDto> = emptyList(),
)

@Keep
@Serializable
data class SevenTVPaintLayerDto(
    val id: String,
    val opacity: Float = 1f,
    val ty: SevenTVPaintLayerTypeDto? = null,
)

@Keep
@Serializable
data class SevenTVPaintLayerTypeDto(
    @SerialName("__typename") val typeName: String? = null,
    val angle: Float? = null,
    val repeating: Boolean? = null,
    val shape: String? = null,
    val stops: List<SevenTVPaintStopDto>? = null,
    val color: SevenTVPaintColorDto? = null,
    val images: List<SevenTVPaintImageDto>? = null,
)

@Keep
@Serializable
data class SevenTVPaintStopDto(
    val at: Float,
    val color: SevenTVPaintColorDto,
)

@Keep
@Serializable
data class SevenTVPaintColorDto(
    val hex: String,
)

@Keep
@Serializable
data class SevenTVPaintImageDto(
    val url: String,
    val mime: String? = null,
    val scale: Float? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Keep
@Serializable
data class SevenTVPaintShadowDto(
    val color: SevenTVPaintColorDto,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blur: Float = 0f,
)

@Keep
@Serializable
data class SevenTVUsersConnectionDataDto(
    val users: Map<String, SevenTVUserByConnectionDto?> = emptyMap(),
)

@Keep
@Serializable
data class SevenTVUserByConnectionDto(
    val id: String? = null,
    val style: SevenTVUserStyleGqlDto? = null,
)

@Keep
@Serializable
data class SevenTVUserStyleGqlDto(
    val activePaintId: String? = null,
)
