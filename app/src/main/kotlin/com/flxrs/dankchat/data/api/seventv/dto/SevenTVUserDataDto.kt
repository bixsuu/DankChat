package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVUserDataDto(
    val id: String,
    val connections: List<SevenTVUserConnection>,
    val style: SevenTVUserStyleDto? = null,
)

@Keep
@Serializable
data class SevenTVUserStyleDto(
    @SerialName("paint_id") val paintId: String? = null,
)
