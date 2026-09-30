package com.flxrs.dankchat.data.twitch.paint

import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintDto

data class SevenTVPaint(
    val id: String,
    val name: String,
    val layers: List<SevenTVPaintLayer>,
    val shadows: List<SevenTVPaintShadow>,
)

sealed interface SevenTVPaintLayer {
    val id: String
    val opacity: Float

    data class SingleColor(
        override val id: String,
        override val opacity: Float,
        val colorHex: String,
    ) : SevenTVPaintLayer

    data class LinearGradient(
        override val id: String,
        override val opacity: Float,
        val angle: Float,
        val repeating: Boolean,
        val stops: List<SevenTVPaintColorStop>,
    ) : SevenTVPaintLayer

    data class RadialGradient(
        override val id: String,
        override val opacity: Float,
        val shape: String,
        val repeating: Boolean,
        val stops: List<SevenTVPaintColorStop>,
    ) : SevenTVPaintLayer

    data class Image(
        override val id: String,
        override val opacity: Float,
        val images: List<SevenTVPaintImageSource>,
    ) : SevenTVPaintLayer
}

data class SevenTVPaintColorStop(
    val at: Float,
    val colorHex: String,
)

data class SevenTVPaintImageSource(
    val url: String,
    val mime: String?,
    val scale: Float?,
    val width: Int?,
    val height: Int?,
)

data class SevenTVPaintShadow(
    val colorHex: String,
    val offsetX: Float,
    val offsetY: Float,
    val blur: Float,
)

fun SevenTVPaintDto.toSevenTVPaint(): SevenTVPaint {
    val domainLayers = data.layers.mapNotNull { layerDto ->
        val ty = layerDto.ty ?: return@mapNotNull null
        val typeName = ty.typeName.orEmpty()
        when {
            typeName.contains("LinearGradient", ignoreCase = true) || (ty.angle != null && ty.stops != null) -> {
                val stops = ty.stops?.map { SevenTVPaintColorStop(it.at, it.color.hex) }.orEmpty()
                SevenTVPaintLayer.LinearGradient(
                    id = layerDto.id,
                    opacity = layerDto.opacity,
                    angle = ty.angle ?: 0f,
                    repeating = ty.repeating ?: false,
                    stops = stops,
                )
            }

            typeName.contains("RadialGradient", ignoreCase = true) || (ty.shape != null && ty.stops != null) -> {
                val stops = ty.stops?.map { SevenTVPaintColorStop(it.at, it.color.hex) }.orEmpty()
                SevenTVPaintLayer.RadialGradient(
                    id = layerDto.id,
                    opacity = layerDto.opacity,
                    shape = ty.shape ?: "circle",
                    repeating = ty.repeating ?: false,
                    stops = stops,
                )
            }

            typeName.contains("SingleColor", ignoreCase = true) || ty.color != null -> {
                val hex = ty.color?.hex ?: return@mapNotNull null
                SevenTVPaintLayer.SingleColor(
                    id = layerDto.id,
                    opacity = layerDto.opacity,
                    colorHex = hex,
                )
            }

            typeName.contains("Image", ignoreCase = true) || !ty.images.isNullOrEmpty() -> {
                val images = ty.images
                    ?.map {
                        SevenTVPaintImageSource(
                            url = it.url,
                            mime = it.mime,
                            scale = it.scale,
                            width = it.width,
                            height = it.height,
                        )
                    }.orEmpty()
                SevenTVPaintLayer.Image(
                    id = layerDto.id,
                    opacity = layerDto.opacity,
                    images = images,
                )
            }

            else -> null
        }
    }

    val domainShadows = data.shadows.map { shadowDto ->
        SevenTVPaintShadow(
            colorHex = shadowDto.color.hex,
            offsetX = shadowDto.offsetX,
            offsetY = shadowDto.offsetY,
            blur = shadowDto.blur,
        )
    }

    return SevenTVPaint(
        id = id,
        name = name,
        layers = domainLayers,
        shadows = domainShadows,
    )
}
