package com.retroweather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.retroweather.art.PixelRenderer
import com.retroweather.data.*
import com.retroweather.widget.WeatherWidget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class IntegrationTests {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val store get() = AppStore(context)
    private val place = Place("New York", 40.71, -74.01)
    private fun fresh(source: String = "Open-Meteo") = Weather(18.0, WeatherKind.PARTLY_CLOUDY, true, System.currentTimeMillis(), System.currentTimeMillis(), source, place.key)
    @Before fun clear() { context.getSharedPreferences("retro_weather", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun encryptedKeyAndIndependentWidgetSettingsSurviveNewStore() {
        store.saveFallbackKey("test-secret-not-a-real-key")
        assertEquals("test-secret-not-a-real-key", AppStore(context).fallbackKey())
        assertFalse(context.getSharedPreferences("retro_weather", Context.MODE_PRIVATE).getString("fallback_key", "")!!.contains("test-secret"))
        store.saveConfig(11, WidgetConfig(place = place, shortcutPackage = "com.example.one", shortcutLabel = "One"))
        store.saveConfig(12, WidgetConfig(place = place, fahrenheit = false, shortcutPackage = "com.example.two", shortcutLabel = "Two"))
        assertEquals("com.example.one", AppStore(context).config(11).shortcutPackage)
        assertFalse(AppStore(context).config(12).fahrenheit)
        store.deleteConfig(11)
        assertTrue(store.hasConfig(12))
    }
    @Test fun primaryOutageFallsBackAndRecoveryReturnsToPrimary() = runBlocking {
        store.saveFallbackKey("fake-key")
        var primaryWorks = false
        var primaryCalls = 0
        val api = WeatherApi(Transport { url ->
            val epoch = System.currentTimeMillis() / 1000
            if (url.contains("open-meteo")) {
                primaryCalls++
                if (!primaryWorks) ApiResponse(503, "{}") else ApiResponse(200, """{"current":{"temperature_2m":18,"weather_code":0,"is_day":1,"time":$epoch}}""")
            } else ApiResponse(200, """{"current":{"temp_c":19,"condition":{"code":1003},"is_day":1,"last_updated_epoch":$epoch}}""")
        })
        val result = WeatherRepository(store, api).refresh(place)
        assertEquals("WeatherAPI.com", result.weather?.source)
        assertEquals(1, primaryCalls)
        // Fresh weather is reused without another request.
        WeatherRepository(store, api).refresh(place)
        assertEquals(1, primaryCalls)
        primaryWorks = true
        store.cooldown("primary", 0)
        store.lastAttempt(place.key, 0)
        store.saveWeather(result.weather!!.copy(fetchedAt = 0))
        assertEquals("Open-Meteo", WeatherRepository(store, api).refresh(place).weather?.source)
    }
    @Test fun bothProvidersFailAndRetainOnlyMatchingLocationCache() = runBlocking {
        store.saveWeather(fresh().copy(fetchedAt = 0))
        store.saveFallbackKey("fake-key")
        val repo = WeatherRepository(store, WeatherApi(Transport { ApiResponse(503, "{}") }))
        assertEquals(place.key, repo.refresh(place).weather?.placeKey)
        assertNull(repo.refresh(Place("London", 51.5, -0.1)).weather)
    }
    @Test fun rateLimitsRespectRetryAfter() = runBlocking {
        val start = System.currentTimeMillis()
        WeatherRepository(store, WeatherApi(Transport { ApiResponse(429, "{}", 3_600_000) })).refresh(place)
        assertTrue(store.cooldown("primary") >= start + 3_600_000)
    }
    @Test fun missingKeyDoesNotCallFallback() = runBlocking {
        var calls = 0
        val result = WeatherRepository(store, WeatherApi(Transport { calls++; ApiResponse(503, "{}") })).refresh(place)
        assertEquals(1, calls)
        assertNull(result.weather)
    }
    @Test fun renderingUsesOnlyChosenColorsAndProducesSpriteSheet() {
        val weather = fresh()
        for (layout in Layout.entries) {
            val config = WidgetConfig(layout = layout, iconColor = Color.WHITE, textColor = Color.YELLOW)
            val bitmap = PixelRenderer.render(config, weather, 320, 160)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue(pixels.all { it == Color.TRANSPARENT || it == Color.WHITE || it == Color.YELLOW })
            assertTrue(pixels.any { it == Color.WHITE })
            assertTrue(pixels.any { it == Color.YELLOW })
        }
        val sheet = Bitmap.createBitmap(800, WeatherKind.entries.size * 130 + 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(0xff11151d.toInt())
        val paint = Paint().apply { color = Color.WHITE; textSize = 22f; isAntiAlias = true }
        canvas.drawText("RETRO WEATHER / ORIGINAL 32 x 32 PIXEL GRIDS", 24f, 34f, paint)
        WeatherKind.entries.forEachIndexed { index, kind ->
            val y = 60 + index * 130
            listOf(true, false).forEachIndexed { i, day ->
                canvas.drawBitmap(PixelRenderer.render(WidgetConfig(showTemperature = false), weather.copy(kind = kind, isDay = day), 128, 128), (24 + i * 150).toFloat(), y.toFloat(), null)
            }
            canvas.drawText(kind.label, 350f, y + 70f, paint)
        }
        File(context.getExternalFilesDir(null), "sprite-sheet.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        export("sprite-sheet.png")
    }
    @Test fun configurationScreenDisplaysSavedWeatherAndShortcutControls() {
        store.saveConfig(0, WidgetConfig(place = place))
        store.saveWeather(fresh())
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        val device = UiDevice.getInstance(instrumentation)
        assertTrue(device.wait(Until.hasObject(By.text("Configuration")), 10_000))
        device.waitForIdle()
        device.takeScreenshot(File(context.getExternalFilesDir(null), "configuration.png"))
        export("configuration.png")
        // Scroll the real Compose screen and inspect first-release shortcut controls.
        repeat(12) {
            if (!device.hasObject(By.text("Choose app"))) {
                // Avoid the horizontal carousels and widget selector inside the vertical screen.
                device.swipe(24, device.displayHeight * 4 / 5, 24, device.displayHeight / 4, 35)
                device.waitForIdle()
            }
        }
        assertNotNull(device.findObject(By.text("Choose app")))
        device.findObject(By.text("Choose app")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Choose tap shortcut")), 5000))
        device.pressBack()
    }
    private fun export(name: String) {
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("mkdir -p /sdcard/Download/RetroWeather")
        device.executeShellCommand("cp ${File(context.getExternalFilesDir(null), name).absolutePath} /sdcard/Download/RetroWeather/$name")
    }
}
