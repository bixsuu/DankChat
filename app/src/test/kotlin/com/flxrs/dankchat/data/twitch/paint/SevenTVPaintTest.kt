package com.flxrs.dankchat.data.twitch.paint

import androidx.compose.ui.graphics.SolidColor
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVGqlResponse
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintColorDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintDataDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintLayerDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintLayerTypeDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintShadowDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintStopDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVPaintsDataDto
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVUsersConnectionDataDto
import com.flxrs.dankchat.ui.chat.paint.LinearGradientPaintBrush
import com.flxrs.dankchat.ui.chat.paint.RadialGradientPaintBrush
import com.flxrs.dankchat.ui.chat.paint.toPaintUi
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class SevenTVPaintTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize and map linear gradient paint`() {
        val payload =
            """
            {
              "id": "paint_1",
              "name": "Fire Gradient",
              "data": {
                "layers": [
                  {
                    "id": "l1",
                    "opacity": 0.9,
                    "ty": {
                      "__typename": "PaintLayerTypeLinearGradient",
                      "angle": 45.0,
                      "repeating": false,
                      "stops": [
                        { "at": 0.0, "color": { "hex": "#ff0000ff" } },
                        { "at": 1.0, "color": { "hex": "#ffff00ff" } }
                      ]
                    }
                  }
                ],
                "shadows": [
                  {
                    "color": { "hex": "#00000080" },
                    "offsetX": 1.0,
                    "offsetY": 2.0,
                    "blur": 3.0
                  }
                ]
              }
            }
            """.trimIndent()

        val dto = json.decodeFromString<SevenTVPaintDto>(payload)
        val paint = dto.toSevenTVPaint()

        assertEquals("paint_1", paint.id)
        assertEquals("Fire Gradient", paint.name)
        assertEquals(1, paint.layers.size)
        assertEquals(1, paint.shadows.size)

        val layer = assertIs<SevenTVPaintLayer.LinearGradient>(paint.layers[0])
        assertEquals("l1", layer.id)
        assertEquals(0.9f, layer.opacity)
        assertEquals(45f, layer.angle)
        assertEquals(false, layer.repeating)
        assertEquals(2, layer.stops.size)
        assertEquals("#ff0000ff", layer.stops[0].colorHex)

        val shadow = paint.shadows[0]
        assertEquals("#00000080", shadow.colorHex)
        assertEquals(1f, shadow.offsetX)
        assertEquals(2f, shadow.offsetY)
        assertEquals(3f, shadow.blur)

        val paintUi = paint.toPaintUi()
        val brush = assertIs<LinearGradientPaintBrush>(paintUi.brush)
        assertEquals(45f, brush.angle)
        assertEquals(2, brush.colors.size)
        assertNotNull(paintUi.shadow)
    }

    @Test
    fun `deserialize and map radial gradient paint`() {
        val payload =
            """
            {
              "id": "paint_radial",
              "name": "Radial Neon",
              "data": {
                "layers": [
                  {
                    "id": "l2",
                    "opacity": 1.0,
                    "ty": {
                      "__typename": "PaintLayerTypeRadialGradient",
                      "shape": "circle",
                      "repeating": true,
                      "stops": [
                        { "at": 0.0, "color": { "hex": "#00ff00ff" } },
                        { "at": 1.0, "color": { "hex": "#0000ffff" } }
                      ]
                    }
                  }
                ],
                "shadows": []
              }
            }
            """.trimIndent()

        val dto = json.decodeFromString<SevenTVPaintDto>(payload)
        val paint = dto.toSevenTVPaint()
        val layer = assertIs<SevenTVPaintLayer.RadialGradient>(paint.layers[0])
        assertEquals("circle", layer.shape)
        assertEquals(true, layer.repeating)

        val paintUi = paint.toPaintUi()
        val brush = assertIs<RadialGradientPaintBrush>(paintUi.brush)
        assertEquals(true, brush.repeating)
        assertEquals(2, brush.colors.size)
        assertNull(paintUi.shadow)
    }

    @Test
    fun `deserialize and map single color paint`() {
        val dto = SevenTVPaintDto(
            id = "paint_single",
            name = "Solid Gold",
            data = SevenTVPaintDataDto(
                layers = listOf(
                    SevenTVPaintLayerDto(
                        id = "l3",
                        opacity = 1f,
                        ty = SevenTVPaintLayerTypeDto(
                            typeName = "PaintLayerTypeSingleColor",
                            color = SevenTVPaintColorDto("#ffd700ff"),
                        ),
                    ),
                ),
            ),
        )

        val paint = dto.toSevenTVPaint()
        val layer = assertIs<SevenTVPaintLayer.SingleColor>(paint.layers[0])
        assertEquals("#ffd700ff", layer.colorHex)

        val paintUi = paint.toPaintUi()
        assertIs<SolidColor>(paintUi.brush)
    }

    @Test
    fun `parse GraphQL users response with active paint IDs`() {
        val gqlPayload =
            """
            {
              "data": {
                "users": {
                  "u0": {
                    "id": "7tv_user_1",
                    "style": { "activePaintId": "paint_xyz" }
                  },
                  "u1": {
                    "id": "7tv_user_2",
                    "style": { "activePaintId": null }
                  },
                  "u2": null
                }
              }
            }
            """.trimIndent()

        val response = json.decodeFromString<SevenTVGqlResponse<SevenTVUsersConnectionDataDto>>(gqlPayload)
        val users = response.data?.users
        assertNotNull(users)
        assertEquals("paint_xyz", users["u0"]?.style?.activePaintId)
        assertNull(users["u1"]?.style?.activePaintId)
        assertNull(users["u2"])
    }
}
