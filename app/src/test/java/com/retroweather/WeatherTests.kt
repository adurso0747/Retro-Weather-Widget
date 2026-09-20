package com.retroweather

import com.retroweather.art.PixelArt
import com.retroweather.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WeatherTests {
    @Test fun allWmoCodesHaveAnExplicitMapping() {
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        codes.forEach { assertNotEquals("code $it", WeatherKind.UNKNOWN, Conditions.openMeteo(it)) }
        assertEquals(WeatherKind.UNKNOWN, Conditions.openMeteo(999))
    }
    @Test fun providerMappingsPreserveHazardousConditions() {
        assertEquals(WeatherKind.FREEZING, Conditions.openMeteo(67))
        assertEquals(WeatherKind.HAIL, Conditions.openMeteo(99))
        assertEquals(WeatherKind.FREEZING, Conditions.weatherApi(1201))
        assertEquals(WeatherKind.HEAVY_SNOW, Conditions.weatherApi(1225))
        assertEquals(WeatherKind.THUNDER, Conditions.weatherApi(1276))
    }
    @Test fun weatherApiCodesAreCovered() {
        listOf(1000,1003,1006,1009,1030,1063,1066,1069,1072,1087,1114,1117,1135,1147,1150,1153,1168,1171,
            1180,1183,1186,1189,1192,1195,1198,1201,1204,1207,1210,1213,1216,1219,1222,1225,1237,1240,1243,1246,
            1249,1252,1255,1258,1261,1264,1273,1276,1279,1282).forEach {
            assertNotEquals("code $it", WeatherKind.UNKNOWN, Conditions.weatherApi(it))
        }
    }
    @Test fun temperaturesHandleFreezingNegativeAndRounding() {
        fun weather(c: Double) = Weather(c, WeatherKind.CLEAR, true, 0, 0, "test", "")
        assertEquals("32°F", weather(0.0).temperature(true))
        assertEquals("-40°F", weather(-40.0).temperature(true))
        assertEquals("-40°C", weather(-40.0).temperature(false))
        assertEquals("19°C", weather(18.6).temperature(false))
    }
    @Test fun stalenessUsesDataTimeRatherThanDownloadTime() {
        val weather = Weather(18.0, WeatherKind.CLEAR, true, 1_000, 7_300_000, "test", "")
        assertTrue(weather.stale(7_300_000))
        assertFalse(weather.copy(observedAt = 7_200_000).stale(7_300_000))
    }
    @Test fun configsRoundTripIndependentSettings() {
        val original = WidgetConfig(true, Place("Home", 40.7, -74.0), false, Layout.BOTTOM, 0xffeebb44.toInt(),
            0xff99aabb.toInt(), 0xff112233.toInt(), true, false, .6f, "com.example.weather", "Other weather")
        assertEquals(original, WidgetConfig.from(JSONObject(original.json().toString())))
    }
    @Test fun coordinatesRejectInvalidInputs() {
        listOf(Double.NaN to 0.0, 91.0 to 0.0, 0.0 to 181.0, Double.POSITIVE_INFINITY to 0.0).forEach { (lat, lon) ->
            assertTrue(runCatching { Place("bad", lat, lon) }.isFailure)
        }
        assertEquals("40.71,-74.01", Place("NYC", 40.7128, -74.0060).key)
    }
    @Test fun everySpriteIsVisibleBoundedAndDistinct() {
        val signatures = WeatherKind.entries.map { kind ->
            val grid = PixelArt.sprite(kind, true)
            assertEquals(32, grid.size)
            assertTrue(grid.all { it.size == 32 })
            assertTrue("$kind should have visible pixels", grid.sumOf { row -> row.count { it } } > 10)
            grid.joinToString { row -> row.joinToString("") { if (it) "#" else "." } }
        }
        assertEquals("Weather families must look different", signatures.size, signatures.toSet().size)
        assertFalse(PixelArt.sprite(WeatherKind.CLEAR, true).contentDeepEquals(PixelArt.sprite(WeatherKind.CLEAR, false)))
    }
    @Test fun temperatureGlyphsAreCompleteAndSevenPixelsHigh() {
        "0123456789-°FC?".forEach { char ->
            assertEquals(7, PixelArt.glyphs.getValue(char).size)
            assertTrue(PixelArt.glyphs.getValue(char).all { it.length == 5 })
        }
    }
    @Test fun openMeteoParsingConvertsSecondsAndNight() {
        val now = 1_800_000_000_000L
        val json = JSONObject("""{"current":{"temperature_2m":-3.2,"weather_code":71,"is_day":0,"time":1800000000}}""")
        val weather = WeatherApi.parseOpenMeteo(json, "test", now)
        assertEquals(now, weather.observedAt)
        assertFalse(weather.isDay)
        assertEquals(WeatherKind.SNOW, weather.kind)
    }
    @Test fun fallbackParsingUsesItsOwnConditionCodes() {
        val now = 1_800_000_000_000L
        val json = JSONObject("""{"current":{"temp_c":22,"condition":{"code":1003},"is_day":1,"last_updated_epoch":1800000000}}""")
        val weather = WeatherApi.parseWeatherApi(json, "test", now)
        assertEquals(WeatherKind.PARTLY_CLOUDY, weather.kind)
        assertEquals("WeatherAPI.com", weather.source)
    }
    @Test fun staleAndMissingProviderFieldsAreRejected() {
        val json = JSONObject("""{"current":{"temperature_2m":12,"weather_code":0,"is_day":1,"time":1800000000}}""")
        assertTrue(runCatching { WeatherApi.parseOpenMeteo(json, "test", 1_800_008_000_000L) }.isFailure)
        assertTrue(runCatching { WeatherApi.parseOpenMeteo(JSONObject("{}"), "test", 0) }.isFailure)
    }
}
