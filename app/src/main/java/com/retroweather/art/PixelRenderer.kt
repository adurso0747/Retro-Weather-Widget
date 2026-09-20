package com.retroweather.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.retroweather.data.*
import kotlin.math.min

object PixelRenderer {
    fun render(config: WidgetConfig, weather: Weather?, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceIn(32, 1200), height.coerceIn(32, 800), Bitmap.Config.ARGB_8888)
        bitmap.density = Bitmap.DENSITY_NONE
        val canvas = Canvas(bitmap)
        canvas.drawColor(config.backgroundColor)
        val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
        val temperature = weather?.temperature(config.fahrenheit) ?: "--°${if (config.fahrenheit) "F" else "C"}"
        val iconSize = if (config.showIcon) 32 else 0
        val gap = if (config.showIcon && config.showTemperature) 4 else 0
        val horizontal = config.layout == Layout.LEFT || config.layout == Layout.RIGHT
        val fontScale = if (bitmap.width - 8 >= (if (horizontal) iconSize + gap else 0) + temperature.length * 12 && bitmap.height - 8 >= (if (horizontal) 32 else iconSize + gap + 14)) 2 else 1
        val textHeight = 7 * fontScale
        val textWidth = if (config.showTemperature) (temperature.length * 6 - 1) * fontScale else 0
        val logicalWidth = if (horizontal) iconSize + gap + textWidth else maxOf(iconSize, textWidth)
        val logicalHeight = if (horizontal) maxOf(iconSize, if (config.showTemperature) textHeight else 0)
            else iconSize + gap + if (config.showTemperature) textHeight else 0
        val availableW = (bitmap.width - 8).coerceAtLeast(1)
        val availableH = (bitmap.height - 8).coerceAtLeast(1)
        val factor = (min(availableW.toFloat() / logicalWidth.coerceAtLeast(1), availableH.toFloat() / logicalHeight.coerceAtLeast(1)) * config.scale).toInt().coerceAtLeast(1)
        val startX = (bitmap.width - logicalWidth * factor) / 2
        val startY = (bitmap.height - logicalHeight * factor) / 2
        fun pixel(x: Int, y: Int, color: Int) {
            paint.color = color
            canvas.drawRect((startX + x * factor).toFloat(), (startY + y * factor).toFloat(),
                (startX + (x + 1) * factor).toFloat(), (startY + (y + 1) * factor).toFloat(), paint)
        }
        val iconX = if (horizontal) { if (config.layout == Layout.RIGHT && config.showTemperature) textWidth + gap else 0 } else (logicalWidth - iconSize) / 2
        val iconY = if (horizontal) 0 else if (config.layout == Layout.BOTTOM && config.showTemperature) textHeight + gap else 0
        if (config.showIcon) PixelArt.sprite(weather?.kind ?: WeatherKind.UNKNOWN, weather?.isDay ?: true)
            .forEachIndexed { y, row -> row.forEachIndexed { x, on -> if (on) pixel(iconX + x, iconY + y, config.iconColor) } }
        val textX = if (horizontal) { if (config.layout == Layout.LEFT) iconSize + gap else 0 } else (logicalWidth - textWidth) / 2
        val textY = if (horizontal) (logicalHeight - textHeight) / 2 else if (config.layout == Layout.TOP) iconSize + gap else 0
        if (config.showTemperature) temperature.forEachIndexed { i, char ->
            PixelArt.glyphs.getValue(char).forEachIndexed { y, row -> row.forEachIndexed { x, c ->
                if (c == '1') for (dy in 0 until fontScale) for (dx in 0 until fontScale)
                    pixel(textX + (i * 6 + x) * fontScale + dx, textY + y * fontScale + dy, config.textColor)
            } }
        }
        if (weather?.stale() == true) {
            paint.color = 0xffffbd66.toInt()
            canvas.drawRect((bitmap.width - 8).toFloat(), 3f, (bitmap.width - 3).toFloat(), 8f, paint)
        }
        return bitmap
    }
}
