package com.flxrs.dankchat.data.api.ffz.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonPrimitive

@Keep
@Serializable
data class FFZGlobalDto(
    @SerialName(value = "default_sets") val defaultSets: List<JsonPrimitive> = emptyList(),
    @SerialName(value = "sets") val sets: Map<String, FFZEmoteSetDto> = emptyMap(),
) {
    val defaultSetIds: Set<String>
        get() = defaultSets.map { it.content }.toSet()
}
