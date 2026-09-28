package com.retroweather

import com.retroweather.art.PixelArt
import com.retroweather.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AppearanceTests {
    @Test fun oldWidgetSettingsKeepTheirLocationShortcutAndStyle() {
        val old = JSONObject("""{"place":{"name":"London","lat":51.5,"lon":-0.1},"layout":"RIGHT","iconColor":-65536,"shortcutPackage":"example.weather","shortcutLabel":"Weather"}""")
        val restored = WidgetConfig.from(old)
        assertEquals(Appearance(), restored.appearance)
        assertEquals("London", restored.place?.name)
        assertEquals(Layout.RIGHT, restored.layout)
        assertEquals(-65536, restored.iconColor)
        assertEquals("example.weather", restored.shortcutPackage)
    }
    @Test fun allAppearanceOptionsSurvivePersistence() {
        val style = Appearance(theme = IconTheme.SOLID, iconScale = 3, textScale = 4, iconPadding = 2, textPadding = 3,
            outerPadding = 16, horizontal = Position.START, vertical = Position.END,
            iconHorizontal = Position.END, iconVertical = Position.START, textHorizontal = Position.START, textVertical = Position.END,
            dynamicSizing = false, fixedPixelSize = 6,
            icon = ColorTreatment(listOf(-65536, -16711936, -16776961), 45, GradientDirection.HORIZONTAL),
            text = ColorTreatment(listOf(-1), 70, GradientDirection.VERTICAL),
            background = ColorTreatment(listOf(-256), 30), outlines = true, outlineColor = -1, cutout = true)
        val config = WidgetConfig(appearance = style)
        assertEquals(config, WidgetConfig.from(JSONObject(config.json().toString())))
    }
    @Test fun corruptOptionsHaveBoundedFallbacks() {
        val restored = Appearance.from(JSONObject("""{"theme":"future","horizontal":"future","iconScale":400,"textScale":-2,"outerPadding":-99,"icon":{"opacity":999,"stops":[1,2,3,4,5],"direction":"future"}}"""))
        assertEquals(IconTheme.OUTLINE, restored.theme)
        assertEquals(Position.CENTER, restored.horizontal)
        assertEquals(3, restored.iconScale)
        assertEquals(1, restored.textScale)
        assertEquals(0, restored.outerPadding)
        assertEquals(100, restored.icon.opacity)
        assertEquals(3, restored.icon.stops.size)
        assertEquals(GradientDirection.DIAGONAL, restored.icon.direction)
    }
    @Test fun solidArtworkIsHandPlacedAndHasMoreFilledCells() {
        for (kind in WeatherKind.entries.filter { it != WeatherKind.FOG && it != WeatherKind.UNKNOWN }) for (day in listOf(true, false)) {
            val outline = PixelArt.sprite(kind, day, IconTheme.OUTLINE)
            val solid = PixelArt.sprite(kind, day, IconTheme.SOLID)
            assertEquals(32, solid.size)
            assertTrue(solid.all { it.size == 32 })
            assertTrue("$kind / $day", solid.sumOf { row -> row.count { it } } > outline.sumOf { row -> row.count { it } })
        }
    }
}
