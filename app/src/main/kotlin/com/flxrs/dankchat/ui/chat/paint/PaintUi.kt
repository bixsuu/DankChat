package com.flxrs.dankchat.ui.chat.paint

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaint
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaintColorStop
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaintLayer
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaintShadow
import com.flxrs.dankchat.ui.chat.emote.emoteBaseHeight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Immutable
data class PaintUi(
    val brush: Brush? = null,
    val shadow: Shadow? = null,
    val layer: SevenTVPaintLayer? = null,
    val shadows: List<SevenTVPaintShadow> = emptyList(),
) {
    fun toBrush(bounds: Rect? = null): Brush? {
        if (bounds == null) return brush
        val l = layer ?: return brush
        return layerToBrush(l, bounds)
    }

    fun toShadow(density: Density? = null): Shadow? {
        if (shadows.isNotEmpty()) {
            return shadowsToUi(shadows, density)
        }
        return shadow
    }
}

fun SevenTVPaint.toPaintUi(): PaintUi {
    val primaryLayer = layers.firstOrNull()
    val brush = primaryLayer?.let { layerToBrush(it) }
    val shadow = shadowsToUi(shadows)

    return PaintUi(
        brush = brush,
        shadow = shadow,
        layer = primaryLayer,
        shadows = shadows,
    )
}

fun normalizeGradientStops(
    stops: List<SevenTVPaintColorStop>,
    opacity: Float = 1f,
    repeating: Boolean = false,
): Triple<List<Color>, List<Float>, Float>? {
    if (stops.isEmpty()) return null

    val validPairs = stops.mapNotNull { stop ->
        val color = parseCssHexColor(stop.colorHex, opacity) ?: return@mapNotNull null
        stop.at.coerceIn(0f, 1f) to color
    }
    if (validPairs.isEmpty()) return null

    // CSS spec: each stop position must be >= previous stop position
    var maxAt = 0f
    val monotonicPairs = validPairs
        .map { (at, color) ->
            val adjustedAt = maxOf(at, maxAt)
            maxAt = adjustedAt
            adjustedAt to color
        }.toMutableList()

    // Ensure first stop starts at 0.0f
    if (monotonicPairs.first().first > 0f) {
        monotonicPairs.add(0, 0f to monotonicPairs.first().second)
    }

    val lastAt = monotonicPairs.last().first
    val repeatScale = if (repeating && lastAt > 0f && lastAt < 1f) {
        lastAt
    } else {
        1f
    }

    if (repeating && repeatScale < 1f) {
        // For repeating gradients, scale stop positions into [0, 1] relative to the repeat period (lastAt)
        val scaledPairs = monotonicPairs.map { (at, color) ->
            (at / repeatScale).coerceIn(0f, 1f) to color
        }
        val finalColors = scaledPairs.map { it.second }
        val finalStops = scaledPairs.map { it.first }
        return Triple(finalColors, finalStops, repeatScale)
    }

    // Ensure last stop ends at 1.0f for non-repeating (or when repeat reaches 1.0)
    if (monotonicPairs.last().first < 1f) {
        monotonicPairs.add(1f to monotonicPairs.last().second)
    }

    if (monotonicPairs.size < 2) {
        val color = monotonicPairs.first().second
        return Triple(listOf(color, color), listOf(0f, 1f), 1f)
    }

    val finalColors = monotonicPairs.map { it.second }
    val finalStops = monotonicPairs.map { it.first }
    return Triple(finalColors, finalStops, repeatScale)
}

internal fun layerToBrush(
    layer: SevenTVPaintLayer,
    bounds: Rect? = null,
): Brush? {
    return when (layer) {
        is SevenTVPaintLayer.SingleColor -> {
            val color = parseCssHexColor(layer.colorHex, layer.opacity) ?: return null
            SolidColor(color)
        }

        is SevenTVPaintLayer.LinearGradient -> {
            val (colors, stops, repeatScale) = normalizeGradientStops(layer.stops, layer.opacity, layer.repeating) ?: return null
            LinearGradientPaintBrush(
                angle = layer.angle,
                repeating = layer.repeating,
                colors = colors,
                stops = stops,
                repeatScale = repeatScale,
                bounds = bounds,
            )
        }

        is SevenTVPaintLayer.RadialGradient -> {
            val (colors, stops, repeatScale) = normalizeGradientStops(layer.stops, layer.opacity, layer.repeating) ?: return null
            RadialGradientPaintBrush(
                repeating = layer.repeating,
                colors = colors,
                stops = stops,
                repeatScale = repeatScale,
                bounds = bounds,
            )
        }

        is SevenTVPaintLayer.Image -> null
    }
}

fun shadowsToUi(
    shadows: List<SevenTVPaintShadow>,
    density: Density? = null,
): Shadow? {
    if (shadows.isEmpty()) return null
    // Pick the most prominent shadow (largest blur radius for outer glow aura)
    val bestShadow = shadows.maxByOrNull { it.blur } ?: return null
    val color = parseCssHexColor(bestShadow.colorHex) ?: return null

    val densityFactor = density?.density ?: 2.75f
    // Scale blur from CSS px to device pixels with glow expansion factor (matching 7TV / desktop clients)
    val scaledBlur = if (bestShadow.blur > 0f) {
        (bestShadow.blur * densityFactor * 2.5f).coerceAtLeast(bestShadow.blur * 2f)
    } else {
        0f
    }
    val offsetX = bestShadow.offsetX * densityFactor
    val offsetY = bestShadow.offsetY * densityFactor

    return Shadow(
        color = color,
        offset = Offset(offsetX, offsetY),
        blurRadius = scaledBlur,
    )
}

class LinearGradientPaintBrush(
    val angle: Float,
    val repeating: Boolean,
    val colors: List<Color>,
    val stops: List<Float>?,
    val repeatScale: Float = 1f,
    val bounds: Rect? = null,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val startX = bounds?.left ?: 0f
        val startY = bounds?.top ?: 0f
        val width = bounds?.width ?: if (size.isSpecified && size.width > 0f) {
            if (repeating || size.width > 400f) minOf(size.width, 240f) else size.width
        } else {
            200f
        }
        val height = bounds?.height ?: if (size.isSpecified && size.height > 0f) {
            if (size.height > 60f) minOf(size.height, 40f) else size.height
        } else {
            40f
        }

        val rad = (angle % 360.0) * (PI / 180.0)
        val sinA = sin(rad).toFloat()
        val cosA = cos(rad).toFloat()
        val dx = sinA
        val dy = -cosA
        val length = (abs(width * sinA) + abs(height * cosA)) * repeatScale
        val halfLen = length / 2f
        val cx = startX + width / 2f
        val cy = startY + height / 2f
        val start = Offset(cx - dx * halfLen, cy - dy * halfLen)
        val end = Offset(cx + dx * halfLen, cy + dy * halfLen)
        val tileMode = if (repeating) TileMode.Repeated else TileMode.Clamp

        return LinearGradientShader(
            from = start,
            to = end,
            colors = colors,
            colorStops = stops,
            tileMode = tileMode,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearGradientPaintBrush) return false
        return angle == other.angle &&
            repeating == other.repeating &&
            colors == other.colors &&
            stops == other.stops &&
            repeatScale == other.repeatScale &&
            bounds == other.bounds
    }

    override fun hashCode(): Int {
        var result = angle.hashCode()
        result = 31 * result + repeating.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + repeatScale.hashCode()
        result = 31 * result + (bounds?.hashCode() ?: 0)
        return result
    }
}

class RadialGradientPaintBrush(
    val repeating: Boolean,
    val colors: List<Color>,
    val stops: List<Float>?,
    val repeatScale: Float = 1f,
    val bounds: Rect? = null,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val startX = bounds?.left ?: 0f
        val startY = bounds?.top ?: 0f
        val width = bounds?.width ?: if (size.isSpecified && size.width > 0f) {
            if (repeating || size.width > 300f) minOf(size.width, 200f) else size.width
        } else {
            160f
        }
        val height = bounds?.height ?: if (size.isSpecified && size.height > 0f) {
            if (size.height > 60f) minOf(size.height, 40f) else size.height
        } else {
            40f
        }

        val cx = startX + width / 2f
        val cy = startY + height / 2f
        val center = Offset(cx, cy)
        val baseRadius = hypot(width / 2f, height / 2f)
        val radius = (baseRadius * repeatScale).coerceAtLeast(1f)
        val tileMode = if (repeating) TileMode.Repeated else TileMode.Clamp

        return RadialGradientShader(
            center = center,
            radius = radius,
            colors = colors,
            colorStops = stops,
            tileMode = tileMode,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RadialGradientPaintBrush) return false
        return repeating == other.repeating &&
            colors == other.colors &&
            stops == other.stops &&
            repeatScale == other.repeatScale &&
            bounds == other.bounds
    }

    override fun hashCode(): Int {
        var result = repeating.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + repeatScale.hashCode()
        result = 31 * result + (bounds?.hashCode() ?: 0)
        return result
    }
}

fun parseCssHexColor(
    hex: String,
    opacity: Float = 1f,
): Color? {
    val clean = hex.removePrefix("#")
    val clampedOpacity = opacity.coerceIn(0f, 1f)
    return when (clean.length) {
        6 -> {
            val r = clean.substring(0, 2).toIntOrNull(16) ?: return null
            val g = clean.substring(2, 4).toIntOrNull(16) ?: return null
            val b = clean.substring(4, 6).toIntOrNull(16) ?: return null
            Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = clampedOpacity)
        }

        8 -> {
            val r = clean.substring(0, 2).toIntOrNull(16) ?: return null
            val g = clean.substring(2, 4).toIntOrNull(16) ?: return null
            val b = clean.substring(4, 6).toIntOrNull(16) ?: return null
            val a = clean.substring(6, 8).toIntOrNull(16) ?: return null
            val finalAlpha = (a / 255f) * clampedOpacity
            Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = finalAlpha)
        }

        3 -> {
            val r = clean.substring(0, 1).repeat(2).toIntOrNull(16) ?: return null
            val g = clean.substring(1, 2).repeat(2).toIntOrNull(16) ?: return null
            val b = clean.substring(2, 3).repeat(2).toIntOrNull(16) ?: return null
            Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = clampedOpacity)
        }

        4 -> {
            val r = clean.substring(0, 1).repeat(2).toIntOrNull(16) ?: return null
            val g = clean.substring(1, 2).repeat(2).toIntOrNull(16) ?: return null
            val b = clean.substring(2, 3).repeat(2).toIntOrNull(16) ?: return null
            val a = clean.substring(3, 4).repeat(2).toIntOrNull(16) ?: return null
            val finalAlpha = (a / 255f) * clampedOpacity
            Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = finalAlpha)
        }

        else -> null
    }
}

fun paintSpanStyle(
    paint: PaintUi?,
    baseColor: Color,
    fontWeight: FontWeight = FontWeight.Bold,
    bounds: Rect? = null,
    density: Density? = null,
): SpanStyle {
    val brush = paint?.toBrush(bounds)
    val shadow = paint?.toShadow(density) ?: paint?.shadow
    return if (brush != null) {
        SpanStyle(
            fontWeight = fontWeight,
            brush = brush,
            shadow = shadow,
        )
    } else {
        SpanStyle(
            fontWeight = fontWeight,
            color = baseColor,
            shadow = shadow,
        )
    }
}

fun measureUsernameBounds(
    textMeasurer: TextMeasurer,
    density: Density,
    fontSize: Float,
    username: String,
    timestamp: String = "",
    badgeCount: Int = 0,
    channelPrefix: String = "",
): Rect {
    var startX = 0f
    if (channelPrefix.isNotEmpty()) {
        val prefix = if (channelPrefix.endsWith(" ")) channelPrefix else "$channelPrefix "
        startX += textMeasurer
            .measure(
                text = prefix,
                style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold),
            ).size.width
    }
    if (timestamp.isNotEmpty()) {
        val tsWidth = textMeasurer
            .measure(
                text = timestamp,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSize * 0.95f).sp,
                    letterSpacing = (-0.03).em,
                ),
            ).size.width
        val spacerWidth = with(density) { 6.dp.toPx() }
        startX += tsWidth + spacerWidth
    }
    if (badgeCount > 0) {
        val badgeWidth = with(density) { emoteBaseHeight(fontSize).toPx() }
        val spaceWidth = textMeasurer.measure(" ", TextStyle(fontSize = fontSize.sp)).size.width
        startX += badgeCount * (badgeWidth + spaceWidth)
    }

    val nameMeasured = textMeasurer.measure(
        text = username,
        style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold),
    )
    val width = nameMeasured.size.width.toFloat()
    val height = nameMeasured.size.height.toFloat()

    return Rect(
        left = startX,
        top = 0f,
        right = startX + width,
        bottom = height,
    )
}
