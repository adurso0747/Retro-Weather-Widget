package com.retroweather.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.retroweather.data.*
import kotlin.math.min
import kotlin.math.roundToInt

/** All visible art cells have integer bounds and a single color, including gradient cells. */
object PixelRenderer {
    fun render(config: WidgetConfig, weather: Weather?, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceIn(32, 1200), height.coerceIn(32, 800), Bitmap.Config.ARGB_8888)
        bitmap.density = Bitmap.DENSITY_NONE
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
        val a = config.appearance
        val hasBackground = config.backgroundColor != 0 && a.background.opacity > 0
        if (hasBackground) {
            // Background gradients are sampled in square cells to preserve the pixel style.
            for (y in 0 until bitmap.height step 4) for (x in 0 until bitmap.width step 4) {
                paint.color = colorAt(config.backgroundColor, a.background, x.toFloat() / bitmap.width, y.toFloat() / bitmap.height)
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 4).toFloat(), (y + 4).toFloat(), paint)
            }
        }
        val text = WidgetText.resolve(config, weather)
        var iconScale = a.iconScale.coerceIn(1, 3)
        var textScale = a.textScale.coerceIn(1, 4)
        var ip = a.iconPadding.coerceIn(0, 8)
        var tp = a.textPadding.coerceIn(0, 8)
        var showIcon = config.showIcon
        var showCondition = config.showCondition && showIcon
        val showText = config.showTemperature
        val horizontal = config.layout == Layout.LEFT || config.layout == Layout.RIGHT
        // Reduce requested sizes before omitting secondary content on small hosts.
        val outer = a.outerPadding.coerceIn(0, min(min(bitmap.width, bitmap.height) / 6, ((min(bitmap.width, bitmap.height) - 34) / 2).coerceAtLeast(0)))
        val availableW = bitmap.width - outer * 2
        val availableH = bitmap.height - outer * 2
        val rim = if (a.outlines) 1 else 0
        var textBlock = PixelText.Block(emptyList())
        var conditionBlock = PixelText.Block(emptyList())
        fun sizes(): IntArray {
            conditionBlock = if (showCondition) PixelText.layout(weather?.kind?.label ?: "Unavailable",
                (minOf(60, availableW - ip * 2 - rim * 2) + 1) / 6, 3) else PixelText.Block(emptyList())
            val iw = if (showIcon) maxOf(32 * iconScale, conditionBlock.width) + ip * 2 else 0
            val ih = if (showIcon) 32 * iconScale + ip * 2 + (if (showCondition) conditionBlock.height + 3 else 0) else 0
            val gap = if (showIcon && showText) 4 else 0
            val textW = (availableW - rim * 2 - tp * 2 - if (horizontal) iw + gap else 0).coerceAtLeast(5 * textScale)
            val textH = (availableH - rim * 2 - tp * 2 - if (!horizontal) ih + gap else 0).coerceAtLeast(7 * textScale)
            textBlock = if (showText) PixelText.layout(text, ((textW / textScale + 1) / 6).coerceAtLeast(1),
                ((textH / textScale + 2) / 9).coerceIn(1, 4)) else PixelText.Block(emptyList())
            val tw = if (showText) textBlock.width * textScale + tp * 2 else 0
            val th = if (showText) textBlock.height * textScale + tp * 2 else 0
            return intArrayOf(iw, tw, th, gap, (if (horizontal) iw + tw + gap else maxOf(iw, tw)) + rim * 2,
                (if (horizontal) maxOf(ih, th) else ih + th + gap) + rim * 2, ih)
        }
        var size = sizes()
        while ((size[4] > availableW || size[5] > availableH) && (iconScale > 1 || textScale > 1 || ip > 0 || tp > 0)) {
            if (iconScale > 1) iconScale--
            if (textScale > 1) textScale--
            ip = 0; tp = 0
            size = sizes()
        }
        if ((size[4] > availableW || size[5] > availableH) && showCondition) {
            showCondition = false
            size = sizes()
        }
        if ((size[4] > availableW || size[5] > availableH) && showIcon && showText) {
            // A very narrow host displays the main text until it is enlarged.
            showIcon = false
            size = sizes()
        }
        val iw = size[0]; val tw = size[1]; val th = size[2]; val gap = size[3]; val ih = size[6]
        val logicalW = size[4]; val logicalH = size[5]
        val fit = min(availableW.toFloat() / logicalW.coerceAtLeast(1), availableH.toFloat() / logicalH.coerceAtLeast(1))
        val factor = if (a.dynamicSizing) (fit * config.scale).toInt().coerceAtLeast(1)
            else a.fixedPixelSize.coerceIn(1, fit.toInt().coerceAtLeast(1))
        val startX = outer + a.horizontal.offset(availableW - logicalW * factor)
        val startY = outer + a.vertical.offset(availableH - logicalH * factor)
        val innerW = logicalW - rim * 2
        val innerH = logicalH - rim * 2
        val iconX = rim + ip + if (horizontal) { if (config.layout == Layout.RIGHT) tw + gap else 0 } else a.iconHorizontal.offset(innerW - iw)
        val iconY = rim + ip + if (horizontal) a.iconVertical.offset(innerH - ih) else if (config.layout == Layout.BOTTOM) th + gap else 0
        val textX = rim + tp + if (horizontal) { if (config.layout == Layout.LEFT && showIcon) iw + gap else 0 } else a.textHorizontal.offset(innerW - tw)
        val textY = rim + tp + if (horizontal) a.textVertical.offset(innerH - th) else if (config.layout == Layout.TOP && showIcon) ih + gap else 0
        val icons = mutableSetOf<Pair<Int, Int>>()
        val letters = mutableSetOf<Pair<Int, Int>>()
        if (showIcon) PixelArt.sprite(weather?.kind ?: WeatherKind.UNKNOWN, weather?.isDay ?: true, a.theme)
            .forEachIndexed { y, row -> row.forEachIndexed { x, on -> if (on) {
                for (dy in 0 until iconScale) for (dx in 0 until iconScale) icons.add(iconX + (iw - ip * 2 - 32 * iconScale) / 2 + x * iconScale + dx to iconY + y * iconScale + dy)
            } } }
        fun addText(block: PixelText.Block, xStart: Int, yStart: Int, scale: Int, target: MutableSet<Pair<Int, Int>>, centered: Boolean = false) {
            block.lines.forEachIndexed { line, value -> value.forEachIndexed { i, char ->
                val offset = if (centered) (block.width - (value.length * 6 - 1).coerceAtLeast(0)) / 2 else 0
                PixelText.glyph(char).forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c == '1') {
                    for (dy in 0 until scale) for (dx in 0 until scale) target.add(xStart + (offset + i * 6 + x) * scale + dx to yStart + (line * 9 + y) * scale + dy)
                } } }
            } }
        }
        if (showText) addText(textBlock, textX, textY, textScale, letters)
        if (showIcon && showCondition) addText(conditionBlock, iconX + (iw - ip * 2 - conditionBlock.width) / 2, iconY + 32 * iconScale + 3, 1, icons, centered = true)
        fun drawCell(x: Int, y: Int) = canvas.drawRect((startX + x * factor).toFloat(), (startY + y * factor).toFloat(),
            (startX + (x + 1) * factor).toFloat(), (startY + (y + 1) * factor).toFloat(), paint)
        if (a.outlines) {
            val all = icons + letters
            val border = mutableSetOf<Pair<Int, Int>>()
            all.forEach { (x, y) -> for (dy in -1..1) for (dx in -1..1) if ((x + dx to y + dy) !in all) border.add(x + dx to y + dy) }
            paint.color = a.outlineColor
            border.forEach { (x, y) -> drawCell(x, y) }
        }
        // Invalid persisted cutout settings use normal content instead of an invisible widget.
        if (a.cutout && (hasBackground || a.outlines)) paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        fun content(cells: Set<Pair<Int, Int>>, base: Int, treatment: ColorTreatment) {
            if (cells.isEmpty()) return
            val minX = cells.minOf { it.first }; val minY = cells.minOf { it.second }
            val spanX = (cells.maxOf { it.first } - minX).coerceAtLeast(1)
            val spanY = (cells.maxOf { it.second } - minY).coerceAtLeast(1)
            cells.forEach { (x, y) ->
                paint.color = colorAt(base, treatment, (x - minX).toFloat() / spanX, (y - minY).toFloat() / spanY)
                drawCell(x, y)
            }
        }
        content(icons, config.iconColor, a.icon)
        content(letters, config.textColor, a.text)
        paint.xfermode = null
        if (weather?.stale() == true) {
            paint.color = 0xffffbd66.toInt()
            canvas.drawRect((bitmap.width - 8).toFloat(), 3f, (bitmap.width - 3).toFloat(), 8f, paint)
        }
        return bitmap
    }

    private fun colorAt(base: Int, treatment: ColorTreatment, x: Float, y: Float): Int {
        val last = minOf(treatment.stops.size, 3)
        fun color(index: Int) = if (index == 0) base else treatment.stops[index - 1]
        val progress = when (treatment.direction) {
            GradientDirection.HORIZONTAL -> x
            GradientDirection.VERTICAL -> y
            GradientDirection.DIAGONAL -> (x + y) / 2
        }.coerceIn(0f, 1f) * last
        val i = progress.toInt().coerceAtMost(last)
        val j = minOf(i + 1, last)
        val mix = progress - i
        fun channel(shift: Int): Int {
            val start = (color(i) ushr shift) and 255
            val end = (color(j) ushr shift) and 255
            return (start + (end - start) * mix).roundToInt()
        }
        val alpha = (channel(24) * treatment.opacity.coerceIn(0, 100) / 100f).roundToInt()
        return (alpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
