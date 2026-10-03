package com.retroweather

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.retroweather.art.PixelRenderer
import com.retroweather.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppearanceDeviceTests {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    private val place = Place("New York", 40.71, -74.01)
    private fun weather() = Weather(18.0, WeatherKind.RAIN, true, System.currentTimeMillis(), System.currentTimeMillis(), "Preview", place.key)
    @Before fun clear() { context.getSharedPreferences("retro_weather", Context.MODE_PRIVATE).edit().clear().commit() }
    private fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }

    @Test fun opacityAndCutoutPreserveTransparentPixels() {
        val config = WidgetConfig(showTemperature = false, appearance = Appearance(icon = ColorTreatment(opacity = 50)))
        val translucent = pixels(PixelRenderer.render(config, weather(), 320, 160))
        assertTrue(translucent.any { Color.alpha(it) == 128 })
        assertTrue(translucent.all { Color.alpha(it) == 0 || Color.alpha(it) == 128 })
        val normal = config.copy(backgroundColor = Color.BLUE, appearance = Appearance())
        val normalPixels = pixels(PixelRenderer.render(normal, weather(), 320, 160))
        val cutout = pixels(PixelRenderer.render(normal.copy(appearance = Appearance(cutout = true)), weather(), 320, 160))
        assertTrue(normalPixels.indices.any { normalPixels[it] == Color.WHITE })
        for (i in normalPixels.indices) {
            if (normalPixels[i] == Color.WHITE) assertEquals(0, Color.alpha(cutout[i]))
            else assertEquals(Color.BLUE, cutout[i])
        }
        // A transparent background with no outline must not erase all content.
        val fallback = pixels(PixelRenderer.render(normal.copy(backgroundColor = 0, appearance = Appearance(cutout = true)), weather(), 320, 160))
        assertTrue(fallback.any { it == Color.WHITE })
    }
    @Test fun gradientAndOutlineRemainSharp() {
        val config = WidgetConfig(showTemperature = false, iconColor = Color.RED,
            appearance = Appearance(icon = ColorTreatment(listOf(Color.BLUE), direction = GradientDirection.HORIZONTAL), outlines = true, outlineColor = Color.GREEN))
        val bitmap = PixelRenderer.render(config, weather(), 320, 160)
        val colors = pixels(bitmap).toSet()
        assertTrue(colors.contains(Color.GREEN))
        assertTrue(colors.contains(Color.RED))
        assertTrue(colors.contains(Color.BLUE))
        assertTrue(colors.size > 10)
        assertTrue(colors.all { Color.alpha(it) == 0 || Color.alpha(it) == 255 })
    }
    @Test fun alignmentPaddingAndElementSizesAffectTheirOwnBounds() {
        fun bounds(bitmap: Bitmap, color: Int): List<Int> {
            val occupied = pixels(bitmap).indices.filter { bitmap.getPixel(it % bitmap.width, it / bitmap.width) == color }
            return listOf(occupied.minOf { it % bitmap.width }, occupied.minOf { it / bitmap.width },
                occupied.maxOf { it % bitmap.width }, occupied.maxOf { it / bitmap.width })
        }
        val base = WidgetConfig(iconColor = Color.WHITE, textColor = Color.YELLOW, appearance = Appearance(dynamicSizing = false, fixedPixelSize = 2))
        val start = PixelRenderer.render(base.copy(appearance = base.appearance.copy(horizontal = Position.START, vertical = Position.START, outerPadding = 12)), weather(), 640, 320)
        val end = PixelRenderer.render(base.copy(appearance = base.appearance.copy(horizontal = Position.END, vertical = Position.END, outerPadding = 12)), weather(), 640, 320)
        assertTrue(bounds(end, Color.WHITE)[0] > bounds(start, Color.WHITE)[0])
        assertTrue(bounds(end, Color.WHITE)[1] > bounds(start, Color.WHITE)[1])
        assertTrue(bounds(start, Color.WHITE)[0] >= 12)
        assertTrue(bounds(end, Color.YELLOW)[2] < 628)
        val small = PixelRenderer.render(base, weather(), 640, 320)
        val large = PixelRenderer.render(base.copy(appearance = base.appearance.copy(iconScale = 2)), weather(), 640, 320)
        assertEquals(pixels(small).count { it == Color.WHITE } * 4, pixels(large).count { it == Color.WHITE })
        assertEquals(pixels(small).count { it == Color.YELLOW }, pixels(large).count { it == Color.YELLOW })
        for (layout in Layout.entries) {
            val single = PixelRenderer.render(base.copy(showIcon = false, layout = layout), weather(), 160, 80)
            assertTrue(pixels(single).any { it == Color.YELLOW })
            assertFalse(pixels(single).any { it == Color.WHITE })
        }
    }
    @Test fun editorChangesArePreviewedAndSavedPerWidget() {
        val store = AppStore(context)
        store.saveConfig(0, WidgetConfig(place = place))
        store.saveWeather(weather())
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        // A cold emulator may spend several seconds verifying Compose classes before its first frame.
        val ready = device.wait(Until.hasObject(By.text("Configuration")), 30_000)
        if (!ready) capture("configuration-launch-failure.png")
        assertTrue("Configuration must appear after cold launch", ready)
        assertTrue(device.hasObject(By.text("Save widget")))
        // Navigate the real setup screen before opening the appearance editor.
        compose.onNodeWithTag("configuration-scroll").performScrollToNode(hasText("Icon settings"))
        compose.onNodeWithText("Icon settings").assertIsDisplayed().performClick()
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNode(hasText("Solid") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        capture("icon-settings.png")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save widget").performClick()
        compose.waitForIdle()
        device.waitForIdle()
        assertEquals(IconTheme.SOLID, AppStore(context).config().appearance.theme)
        // Saving another widget cannot replace the first widget's appearance.
        store.saveConfig(8, store.config().copy(appearance = Appearance(theme = IconTheme.OUTLINE)))
        assertEquals(IconTheme.SOLID, store.config().appearance.theme)
        assertEquals(IconTheme.OUTLINE, store.config(8).appearance.theme)
    }
    @Test fun customTextAndDescriptionRenderAcrossWidgetSizes() {
        val base = WidgetConfig(place = place, showCondition = true, textTemplate = "{temperature:unit}\n{condition}\n{location}",
            iconColor = Color.WHITE, textColor = Color.YELLOW, appearance = Appearance(dynamicSizing = false, fixedPixelSize = 1, outerPadding = 2))
        for (layout in Layout.entries) for ((width, height) in listOf(32 to 32, 160 to 80, 640 to 320)) {
            val bitmap = PixelRenderer.render(base.copy(layout = layout), weather(), width, height)
            assertTrue("$layout $width x $height must retain main text", pixels(bitmap).any { it == Color.YELLOW })
            // Main text must fit inside the actual bitmap, including the smallest supported size.
            for (x in 0 until width) {
                assertNotEquals(Color.YELLOW, bitmap.getPixel(x, 0))
                assertNotEquals(Color.YELLOW, bitmap.getPixel(x, height - 1))
            }
        }
        val iconOnly = base.copy(showTemperature = false)
        val described = pixels(PixelRenderer.render(iconOnly, weather(), 320, 160))
        val plain = pixels(PixelRenderer.render(iconOnly.copy(showCondition = false), weather(), 320, 160))
        assertTrue(described.count { it == Color.WHITE } > plain.count { it == Color.WHITE })
        assertArrayEquals(pixels(PixelRenderer.render(base.copy(showIcon = false), weather(), 320, 160)),
            pixels(PixelRenderer.render(base.copy(showIcon = false, showCondition = false), weather(), 320, 160)))
    }

    @Test fun templateEditorValidatesCancelsAndPersists() {
        val store = AppStore(context)
        store.saveConfig(0, WidgetConfig(place = place, showCondition = true))
        store.saveConfig(8, WidgetConfig(textTemplate = "OTHER"))
        store.saveWeather(weather())
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("Configuration")), 30_000))
        compose.onNodeWithTag("configuration-scroll").performScrollToNode(hasText("Edit widget text"))
        compose.onNodeWithText("Edit widget text").performClick()
        compose.onNodeWithText("Text template").performTextReplacement("{bad}")
        compose.onNodeWithText("Use text").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(WidgetText.DEFAULT, store.config().textTemplate)
        compose.onNodeWithText("Edit widget text").performClick()
        compose.onNodeWithText("Text template").performTextReplacement("{temperature:unit}\n{condition}")
        device.pressBack()
        compose.onNodeWithText("Use text").assertIsDisplayed()
        capture("text-editor.png")
        compose.onNodeWithText("Use text").performClick()
        compose.onNodeWithText("Save widget").performClick()
        compose.waitForIdle()
        val restored = AppStore(context)
        assertEquals("{temperature:unit}\n{condition}", restored.config().textTemplate)
        assertTrue(restored.config().showCondition)
        assertEquals("OTHER", restored.config(8).textTemplate)
        val example = File(context.getExternalFilesDir(null), "custom-text-example.png")
        example.outputStream().use { PixelRenderer.render(restored.config().copy(textTemplate = "{temperature:unit}\n{location}",
            backgroundColor = 0xff11151d.toInt()), weather(), 640, 320).compress(Bitmap.CompressFormat.PNG, 100, it) }
        export(example)
        capture("widget-text.png")
    }

    @Test fun exportAppearanceExamples() {
        // Export the actual launcher drawable so repository artwork stays in sync with the app.
        val appIcon = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        context.getDrawable(R.drawable.ic_launcher)!!.apply {
            setBounds(0, 0, 256, 256)
            draw(Canvas(appIcon))
        }
        val appIconFile = File(context.getExternalFilesDir(null), "app-icon.png")
        appIconFile.outputStream().use { appIcon.compress(Bitmap.CompressFormat.PNG, 100, it) }
        export(appIconFile)
        val examples = listOf(
            "Outline" to WidgetConfig(),
            "Solid" to WidgetConfig(appearance = Appearance(theme = IconTheme.SOLID)),
            "Icon and text gradients" to WidgetConfig(iconColor = 0xffffca65.toInt(), textColor = 0xff99d5bc.toInt(),
                appearance = Appearance(icon = ColorTreatment(listOf(0xffed9ab4.toInt())), text = ColorTreatment(listOf(0xffa6bdff.toInt())))),
            "Contrast outlines" to WidgetConfig(iconColor = 0xff99d5bc.toInt(), appearance = Appearance(outlines = true, outlineColor = 0xff826bae.toInt())),
            "Cutout background" to WidgetConfig(backgroundColor = 0xfff4be65.toInt(),
                appearance = Appearance(cutout = true, background = ColorTreatment(listOf(0xffed9ab4.toInt())))),
            "Top layout / solid / 60% opacity" to WidgetConfig(layout = Layout.TOP,
                appearance = Appearance(theme = IconTheme.SOLID, icon = ColorTreatment(opacity = 60), text = ColorTreatment(opacity = 60)))
        )
        val sheet = Bitmap.createBitmap(800, 660, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet); canvas.drawColor(0xff11151d.toInt())
        val paint = Paint().apply { color = Color.WHITE; textSize = 18f; isAntiAlias = true }
        examples.forEachIndexed { index, (label, config) ->
            val x = (index % 2) * 400
            val y = (index / 2) * 220
            canvas.drawText(label, x + 16f, y + 26f, paint)
            canvas.drawBitmap(PixelRenderer.render(config, weather(), 360, 160), x + 16f, y + 40f, null)
        }
        val file = File(context.getExternalFilesDir(null), "appearance-examples.png")
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        export(file)
        val icons = Bitmap.createBitmap(800, WeatherKind.entries.size * 130 + 60, Bitmap.Config.ARGB_8888)
        val iconCanvas = Canvas(icons); iconCanvas.drawColor(0xff11151d.toInt())
        iconCanvas.drawText("OUTLINE: DAY / NIGHT       SOLID: DAY / NIGHT", 16f, 30f, paint)
        WeatherKind.entries.forEachIndexed { row, kind ->
            IconTheme.entries.forEachIndexed { themeIndex, theme ->
                listOf(true, false).forEachIndexed { dayIndex, day ->
                    val config = WidgetConfig(showTemperature = false, appearance = Appearance(theme = theme))
                    iconCanvas.drawBitmap(PixelRenderer.render(config, weather().copy(kind = kind, isDay = day), 128, 128), (16 + (themeIndex * 2 + dayIndex) * 130).toFloat(), (60 + row * 130).toFloat(), null)
                }
            }
            iconCanvas.drawText(kind.label, 550f, 130f + row * 130, paint)
        }
        val iconFile = File(context.getExternalFilesDir(null), "theme-icons.png")
        iconFile.outputStream().use { icons.compress(Bitmap.CompressFormat.PNG, 100, it) }
        export(iconFile)
    }
    private fun capture(name: String) {
        val file = File(context.getExternalFilesDir(null), name)
        device.takeScreenshot(file); export(file)
    }
    private fun export(file: File) {
        device.executeShellCommand("mkdir -p /sdcard/Download/RetroWeather")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/RetroWeather/${file.name}")
    }
}
