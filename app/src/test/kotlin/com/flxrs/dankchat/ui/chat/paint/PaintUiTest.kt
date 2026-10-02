package com.flxrs.dankchat.ui.chat.paint

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.flxrs.dankchat.data.twitch.paint.SevenTVPaintColorStop
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class PaintUiTest {
    @Test
    fun `parse 6-digit hex color`() {
        val color = parseCssHexColor("#ff0000")
        assertNotNull(color)
        assertEquals(1f, color.red)
        assertEquals(0f, color.green)
        assertEquals(0f, color.blue)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun `parse 8-digit hex color with CSS alpha at the end`() {
        val color = parseCssHexColor("#00ff0080")
        assertNotNull(color)
        assertEquals(0f, color.red)
        assertEquals(1f, color.green)
        assertEquals(0f, color.blue)
        assertTrue(color.alpha in 0.49f..0.51f)
    }

    @Test
    fun `parse 3-digit shorthand hex color`() {
        val color = parseCssHexColor("#f00")
        assertNotNull(color)
        assertEquals(1f, color.red)
        assertEquals(0f, color.green)
        assertEquals(0f, color.blue)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun `parse 4-digit shorthand hex color`() {
        val color = parseCssHexColor("#0f08")
        assertNotNull(color)
        assertEquals(0f, color.red)
        assertEquals(1f, color.green)
        assertEquals(0f, color.blue)
        assertTrue(color.alpha in 0.52f..0.54f)
    }

    @Test
    fun `opacity multiplier is applied to hex alpha`() {
        val color = parseCssHexColor("#ffffff", opacity = 0.5f)
        assertNotNull(color)
        assertEquals(1f, color.red)
        assertEquals(1f, color.green)
        assertEquals(1f, color.blue)
        assertTrue(color.alpha in 0.49f..0.51f)

        val transparentColor = parseCssHexColor("#ffffff80", opacity = 0.5f)
        assertNotNull(transparentColor)
        assertTrue(transparentColor.alpha in 0.24f..0.26f)
    }

    @Test
    fun `invalid hex returns null`() {
        assertNull(parseCssHexColor(""))
        assertNull(parseCssHexColor("#12345"))
        assertNull(parseCssHexColor("#xyzxyz"))
        assertNull(parseCssHexColor("not-a-color"))
    }

    @Test
    fun `paintSpanStyle uses brush when available`() {
        val brush = LinearGradientPaintBrush(
            angle = 90f,
            repeating = false,
            colors = listOf(Color.Red, Color.Blue),
            stops = listOf(0f, 1f),
        )
        val paint = PaintUi(brush = brush)
        val style = paintSpanStyle(paint, baseColor = Color.Green, fontWeight = FontWeight.Bold)

        assertEquals(brush, style.brush)
        assertEquals(Color.Unspecified, style.color)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun `paintSpanStyle uses baseColor when brush is null`() {
        val paint = PaintUi(brush = null)
        val style = paintSpanStyle(paint, baseColor = Color.Green, fontWeight = FontWeight.Bold)

        assertNull(style.brush)
        assertEquals(Color.Green, style.color)
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun `paintSpanStyle propagates shadow`() {
        val shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black,
            offset = Offset(2f, 2f),
            blurRadius = 4f,
        )
        val paint = PaintUi(brush = null, shadow = shadow)
        val style = paintSpanStyle(paint, baseColor = Color.Green)

        assertEquals(shadow, style.shadow)
        assertEquals(Color.Green, style.color)
    }

    @Test
    fun `normalizeGradientStops enforces monotonic non-decreasing order for unsorted stops`() {
        val stops = listOf(
            SevenTVPaintColorStop(at = 0.27f, colorHex = "#ff0000"),
            SevenTVPaintColorStop(at = 0.06f, colorHex = "#00ff00"),
            SevenTVPaintColorStop(at = 0.78f, colorHex = "#0000ff"),
            SevenTVPaintColorStop(at = 1.0f, colorHex = "#ffffff"),
        )
        val result = normalizeGradientStops(stops)
        assertNotNull(result)
        val (colors, stopPositions) = result
        assertEquals(5, stopPositions.size)
        assertEquals(listOf(0f, 0.27f, 0.27f, 0.78f, 1.0f), stopPositions)
        assertEquals(5, colors.size)
        // Monotonic check
        for (i in 1 until stopPositions.size) {
            assertTrue(stopPositions[i] >= stopPositions[i - 1])
        }
    }

    @Test
    fun `normalizeGradientStops pads missing endpoints 0f and 1f`() {
        val stops = listOf(
            SevenTVPaintColorStop(at = 0.2f, colorHex = "#ff0000"),
            SevenTVPaintColorStop(at = 0.8f, colorHex = "#0000ff"),
        )
        val result = normalizeGradientStops(stops)
        assertNotNull(result)
        val (colors, stopPositions) = result
        assertEquals(listOf(0f, 0.2f, 0.8f, 1.0f), stopPositions)
        assertEquals(4, colors.size)
        assertEquals(colors[0], colors[1])
        assertEquals(colors[2], colors[3])
    }

    @Test
    fun `normalizeGradientStops clamps out of bounds stops`() {
        val stops = listOf(
            SevenTVPaintColorStop(at = -0.1f, colorHex = "#ff0000"),
            SevenTVPaintColorStop(at = 1.01f, colorHex = "#0000ff"),
        )
        val result = normalizeGradientStops(stops)
        assertNotNull(result)
        val (colors, stopPositions) = result
        assertEquals(listOf(0f, 1.0f), stopPositions)
        assertEquals(2, colors.size)
    }

    @Test
    fun `normalizeGradientStops returns null for empty or invalid stops`() {
        assertNull(normalizeGradientStops(emptyList()))
        assertNull(normalizeGradientStops(listOf(SevenTVPaintColorStop(at = 0.5f, colorHex = "invalid"))))
    }

    @Test
    fun `linear and radial paint brushes safely create shaders with large and unspecified sizes`() {
        val colors = listOf(Color.Red, Color.Blue)
        val stops = listOf(0f, 1f)

        val linearBrush = LinearGradientPaintBrush(angle = 90f, repeating = false, colors = colors, stops = stops)
        assertNotNull(linearBrush.createShader(Size(1080f, 60f)))
        assertNotNull(linearBrush.createShader(Size.Unspecified))
        assertNotNull(linearBrush.createShader(Size.Zero))

        val radialBrush = RadialGradientPaintBrush(repeating = false, colors = colors, stops = stops)
        assertNotNull(radialBrush.createShader(Size(1080f, 60f)))
        assertNotNull(radialBrush.createShader(Size.Unspecified))
        assertNotNull(radialBrush.createShader(Size.Zero))
    }

    @Test
    fun `normalizeGradientStops scales repeating gradient stops relative to last stop`() {
        val stops = listOf(
            SevenTVPaintColorStop(at = 0.0f, colorHex = "#ff0000"),
            SevenTVPaintColorStop(at = 0.25f, colorHex = "#00ff00"),
            SevenTVPaintColorStop(at = 0.5f, colorHex = "#0000ff"),
        )
        val result = normalizeGradientStops(stops, repeating = true)
        assertNotNull(result)
        val (colors, stopPositions, repeatScale) = result
        assertEquals(0.5f, repeatScale)
        // Scaled to [0, 1] relative to 0.5
        assertEquals(listOf(0f, 0.5f, 1.0f), stopPositions)
        assertEquals(3, colors.size)
    }

    @Test
    fun `linear and radial brushes create shaders using bounds`() {
        val colors = listOf(Color.Red, Color.Blue)
        val stops = listOf(0f, 1f)
        val bounds = androidx.compose.ui.geometry
            .Rect(100f, 10f, 300f, 50f)

        val linearBrush = LinearGradientPaintBrush(
            angle = 125f,
            repeating = false,
            colors = colors,
            stops = stops,
            bounds = bounds,
        )
        assertNotNull(linearBrush.createShader(Size(1080f, 60f)))

        val radialBrush = RadialGradientPaintBrush(
            repeating = true,
            colors = colors,
            stops = stops,
            repeatScale = 0.5f,
            bounds = bounds,
        )
        assertNotNull(radialBrush.createShader(Size(1080f, 60f)))
    }

    @Test
    fun `paintSpanStyle applies bounds to PaintUi toBrush`() {
        val layer = com.flxrs.dankchat.data.twitch.paint.SevenTVPaintLayer.LinearGradient(
            id = "layer1",
            opacity = 1f,
            angle = 90f,
            repeating = false,
            stops = listOf(
                SevenTVPaintColorStop(at = 0f, colorHex = "#ff0000"),
                SevenTVPaintColorStop(at = 1f, colorHex = "#0000ff"),
            ),
        )
        val bounds = androidx.compose.ui.geometry
            .Rect(50f, 0f, 200f, 40f)
        val paint = PaintUi(brush = null, layer = layer)

        val style = paintSpanStyle(paint, baseColor = Color.Green, bounds = bounds)
        assertNotNull(style.brush)
        val linearBrush = style.brush as? LinearGradientPaintBrush
        assertNotNull(linearBrush)
        assertEquals(bounds, linearBrush.bounds)
    }

    @Test
    fun `shadowsToUi scales blur and offset with density`() {
        val shadow = com.flxrs.dankchat.data.twitch.paint.SevenTVPaintShadow(
            colorHex = "#BBFF00FF",
            offsetX = 1f,
            offsetY = 2f,
            blur = 4f,
        )
        val density = androidx.compose.ui.unit.Density(density = 3f)
        val uiShadow = shadowsToUi(listOf(shadow), density)

        assertNotNull(uiShadow)
        assertEquals(Offset(3f, 6f), uiShadow.offset)
        // blur = 4f * 3f * 2.5f = 30f
        assertEquals(30f, uiShadow.blurRadius)
        val parsedColor = parseCssHexColor("#BBFF00FF")
        assertEquals(parsedColor, uiShadow.color)
    }

    @Test
    fun `shadowsToUi selects shadow with largest blur for glow aura`() {
        val edgeShadow = com.flxrs.dankchat.data.twitch.paint.SevenTVPaintShadow(
            colorHex = "#FE7694FF",
            offsetX = 0f,
            offsetY = 0f,
            blur = 0.1f,
        )
        val glowShadow = com.flxrs.dankchat.data.twitch.paint.SevenTVPaintShadow(
            colorHex = "#FF4D73FF",
            offsetX = 0f,
            offsetY = 0f,
            blur = 4f,
        )
        val density = androidx.compose.ui.unit.Density(density = 2f)
        val uiShadow = shadowsToUi(listOf(edgeShadow, glowShadow), density)

        assertNotNull(uiShadow)
        // blur = 4f * 2f * 2.5f = 20f
        assertEquals(20f, uiShadow.blurRadius)
        assertEquals(parseCssHexColor("#FF4D73FF"), uiShadow.color)
    }

    @Test
    fun `PaintUi toShadow re-scales dynamically with density`() {
        val shadow = com.flxrs.dankchat.data.twitch.paint.SevenTVPaintShadow(
            colorHex = "#00FF00FF",
            offsetX = 0f,
            offsetY = 0f,
            blur = 4f,
        )
        val paint = PaintUi(shadows = listOf(shadow))

        val shadow2x = paint.toShadow(androidx.compose.ui.unit.Density(2f))
        val shadow3x = paint.toShadow(androidx.compose.ui.unit.Density(3f))

        assertNotNull(shadow2x)
        assertNotNull(shadow3x)
        assertEquals(20f, shadow2x.blurRadius)
        assertEquals(30f, shadow3x.blurRadius)
    }

    @Test
    fun `paintSpanStyle uses density-scaled shadow`() {
        val shadow = com.flxrs.dankchat.data.twitch.paint.SevenTVPaintShadow(
            colorHex = "#BBFF00FF",
            offsetX = 0f,
            offsetY = 0f,
            blur = 4f,
        )
        val paint = PaintUi(shadows = listOf(shadow))
        val density = androidx.compose.ui.unit.Density(3f)
        val style = paintSpanStyle(paint, baseColor = Color.White, density = density)

        assertNotNull(style.shadow)
        assertEquals(30f, style.shadow?.blurRadius)
    }
}

