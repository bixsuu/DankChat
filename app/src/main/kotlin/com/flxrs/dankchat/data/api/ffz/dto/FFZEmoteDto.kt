package com.flxrs.dankchat.data.api.ffz.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FFZEmoteDto(
    val urls: Map<String, String?> = emptyMap(),
    val animated: Map<String, String?>? = null,
    val name: String,
    val id: Int,
    val owner: FFZEmoteOwnerDto? = null,
    @SerialName(value = "modifier") val isModifier: Boolean = false,
    @SerialName(value = "modifier_flags") val modifierFlags: Int = 0,
)
