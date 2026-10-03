package com.retroweather

import com.retroweather.art.PixelText
import com.retroweather.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetTextTests {
    private val weather = Weather(-5.0, WeatherKind.SNOW, false,
        Instant.parse("2026-10-02T01:30:00Z").toEpochMilli(), 0, "Test", "")

    @Test fun fieldsUseSelectedUnitsAndObservationTimezone() {
        val config = WidgetConfig(place = Place("Boston", 42.0, -71.0), fahrenheit = false,
            textTemplate = "{location}\n{temperature:unit} / {temperature} {unit}\n{condition}\n{date} {updated}")
        assertEquals("Boston\n-5°C / -5 °C\nSnow\n2026-10-01 21:30", WidgetText.resolve(config, weather, ZoneId.of("America/New_York")))
        assertEquals("23°F", WidgetText.resolve(config.copy(fahrenheit = true, textTemplate = WidgetText.DEFAULT), weather))
    }
    @Test fun validationRejectsUnknownMalformedAndUnboundedInput() {
        for (value in listOf("", " ", "{wind}", "{temperature", "temperature}", "{{condition}}", "a".repeat(161), "a\nb\nc\nd\ne", "a\tb"))
            assertNotNull(value, WidgetText.error(value))
        assertNull(WidgetText.error("NOW: {temperature:unit}\n{condition}"))
    }
    @Test fun noDataAndUntrustedLocationAreNotRecursiveTemplates() {
        val config = WidgetConfig(place = Place("{condition}", 0.0, 0.0), textTemplate = "{location}: {temperature:unit} {updated}")
        assertEquals("{condition}: --°F --", WidgetText.resolve(config, null))
        assertEquals("Current location", WidgetText.resolve(config.copy(dynamic = true, textTemplate = "{location}"), weather))
    }
    @Test fun oldConfigsAndInvalidSavedTemplatesRemainUsable() {
        val old = WidgetConfig.from(JSONObject())
        assertEquals(WidgetText.DEFAULT, old.textTemplate)
        assertFalse(old.showCondition)
        assertEquals(WidgetText.DEFAULT, WidgetConfig.from(JSONObject().put("textTemplate", "{bad}")).textTemplate)
        val custom = old.copy(textTemplate = "{condition}\n{temperature:unit}", showCondition = true, shortcutPackage = "example.app")
        assertEquals(custom, WidgetConfig.from(JSONObject(custom.json().toString())))
    }
    @Test fun wrappingHonorsBoundsAndMarksOmittedContent() {
        val block = PixelText.layout("Thunderstorm with hail", 10, 2)
        assertEquals(listOf("THUNDERSTO", "RM WITH..."), block.lines)
        assertTrue(block.width <= 59)
        assertEquals(16, block.height)
        assertEquals(listOf("..."), PixelText.layout("LONGWORD", 3, 1).lines)
        assertEquals(listOf("ONE", "TWO"), PixelText.layout("one\ntwo", 10, 4).lines)
    }
    @Test fun allConditionLabelsHaveHandPlacedGlyphs() {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 °-.,:/!'+()%?"
        for (char in alphabet) {
            assertEquals(7, PixelText.glyph(char).size)
            assertTrue(PixelText.glyph(char).all { it.length == 5 && it.all { bit -> bit == '0' || bit == '1' } })
            if (char != '?' && char != ' ') assertNotEquals(PixelText.glyph('?'), PixelText.glyph(char))
        }
        assertEquals("MONTREAL ?", PixelText.normalize("Montréal 雪"))
        assertTrue(WeatherKind.entries.all { '?' !in PixelText.normalize(it.label) })
    }
}
