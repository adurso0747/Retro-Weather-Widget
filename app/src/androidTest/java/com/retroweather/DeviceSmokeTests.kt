package com.retroweather

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.location.Location
import android.location.LocationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.retroweather.data.*
import com.retroweather.widget.WeatherWidget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

/** Opt-in tests make real network requests and add a widget to the emulator launcher. */
@RunWith(AndroidJUnit4::class)
class DeviceSmokeTests {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    @Before fun optIn() { assumeTrue(InstrumentationRegistry.getArguments().getString("liveNetwork") == "true") }

    @Test fun realWeatherAndGeocodingRespond() = runBlocking {
        val results = WeatherApi().search("New York")
        assertTrue(results.any { it.name.contains("New York") })
        val weather = WeatherApi().current(Place("New York", 40.71, -74.01))
        assertEquals("Open-Meteo", weather.source)
        assertFalse(weather.stale())
        assertNotEquals(WeatherKind.UNKNOWN, weather.kind)
    }

    @Test fun deniedLocationUsesSavedPositionWithoutCrashing() = runBlocking {
        val store = AppStore(context)
        store.saveDynamic(Place("Saved position", 40.71, -74.01), System.currentTimeMillis() - 86_400_000)
        // Fresh installs have no location grant. Don't revoke mid-instrumentation, which kills the process.
        assumeTrue("Run this case on a fresh install before the GPS permission test", !DeviceLocation(context, store).foregroundAllowed())
        val result = DeviceLocation(context, store).resolve(true)
        assertEquals("40.71,-74.01", result.place?.key)
        assertTrue(result.message.contains("permission"))
    }

    @Suppress("DEPRECATION", "MissingPermission")
    @Test fun deviceLocationReadsGrantedGpsFix() = runBlocking {
        device.executeShellCommand("pm grant com.retroweather android.permission.ACCESS_COARSE_LOCATION")
        device.executeShellCommand("pm grant com.retroweather android.permission.ACCESS_FINE_LOCATION")
        device.executeShellCommand("pm grant com.retroweather android.permission.ACCESS_BACKGROUND_LOCATION")
        device.executeShellCommand("appops set com.retroweather android:mock_location allow")
        val manager = context.getSystemService(LocationManager::class.java)
        try {
            manager.addTestProvider("gps", false, false, false, false, true, true, true, 3, 1)
            manager.setTestProviderEnabled("gps", true)
            manager.setTestProviderLocation("gps", Location("gps").apply {
                latitude = 40.7128; longitude = -74.0060; accuracy = 10f
                time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            })
            val resolver = DeviceLocation(context, AppStore(context))
            assertTrue(resolver.foregroundAllowed())
            assertTrue(resolver.backgroundAllowed())
            val result = resolver.resolve(true)
            assertEquals("40.71,-74.01", result.place?.key)
            assertTrue(result.message.isBlank())
        } finally {
            manager.removeTestProvider("gps")
            device.executeShellCommand("appops set com.retroweather android:mock_location default")
        }
    }

    @Test fun pinWidgetAndTapShortcutWithMissingAppRecovery() = runBlocking {
        val store = AppStore(context)
        val place = Place("New York", 40.71, -74.01)
        store.saveWeather(WeatherApi().current(place))
        store.saveConfig(0, WidgetConfig(place = place, shortcutPackage = "com.android.settings", shortcutLabel = "Settings"))
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("Configuration")), 10_000))
        repeat(16) {
            if (!device.hasObject(By.text("Add to home screen"))) {
                // Use the outside gutter so gestures cannot be intercepted by color/layout carousels.
                device.swipe(24, device.displayHeight * 4 / 5, 24, device.displayHeight / 4, 35)
                device.waitForIdle()
            }
        }
        val oldIds = WeatherWidget.ids(context).toSet()
        capture("before-pin")
        assertNotNull("Add widget action must be reachable", device.findObject(By.text("Add to home screen")))
        device.findObject(By.text("Add to home screen")).click()
        device.waitForIdle()
        // Launcher owns the confirmation. Its button text varies by launcher release.
        val home = "com.google.android.apps.nexuslauncher" // Pixel emulator used by this opt-in smoke test.
        assertTrue(device.wait(Until.hasObject(By.pkg(home)), 5000))
        capture("pin-dialog")
        val confirm = device.findObject(By.text(Pattern.compile("Add (automatically|to home screen)", Pattern.CASE_INSENSITIVE)).pkg(home))
        assertNotNull("Launcher pin confirmation must be visible", confirm)
        confirm.click()
        device.waitForIdle()
        device.pressHome()
        assertTrue(device.wait(Until.hasObject(By.res("com.retroweather", "widget_image")), 5000))
        val id = (WeatherWidget.ids(context).toSet() - oldIds).single()
        assertEquals("com.android.settings", store.config(id).shortcutPackage)
        capture("home-widget")
        device.findObject(By.res("com.retroweather", "widget_image")).click()
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 5000))
        // Point the same widget at an unavailable app and confirm the app explains recovery.
        store.saveConfig(id, store.config(id).copy(shortcutPackage = "invalid.missing.app", shortcutLabel = "Missing app"))
        WeatherWidget.render(context, id)
        device.pressHome()
        device.waitForIdle()
        device.findObject(By.res("com.retroweather", "widget_image")).click()
        assertTrue(device.wait(Until.hasObject(By.textContains("Missing app is unavailable")), 5000))
        // Restore the useful shortcut and check resize rendering on the existing widget.
        store.saveConfig(id, store.config(id).copy(shortcutPackage = "com.android.settings", shortcutLabel = "Settings"))
        val manager = AppWidgetManager.getInstance(context)
        manager.updateAppWidgetOptions(id, Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 100)
        })
        WeatherWidget.render(context, id)
        assertTrue(store.hasConfig(id))
    }
    private fun capture(name: String) {
        val file = File(context.getExternalFilesDir(null), "$name.png")
        device.takeScreenshot(file)
        device.executeShellCommand("mkdir -p /sdcard/Download/RetroWeather")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/RetroWeather/$name.png")
        val xml = File(context.getExternalFilesDir(null), "$name.xml")
        device.dumpWindowHierarchy(xml)
        device.executeShellCommand("cp ${xml.absolutePath} /sdcard/Download/RetroWeather/$name.xml")
    }
}
