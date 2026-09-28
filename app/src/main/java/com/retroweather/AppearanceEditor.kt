package com.retroweather

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.retroweather.art.PixelRenderer
import com.retroweather.data.*
import kotlin.math.roundToInt

@Composable internal fun AppearanceEditor(
    page: String, config: WidgetConfig, weather: Weather?, onChange: (WidgetConfig) -> Unit, onDismiss: () -> Unit
) {
    val a = config.appearance
    fun change(next: Appearance) = onChange(config.copy(appearance = next))
    val title = when (page) {
        "icon-style" -> "Icon settings"; "text-style" -> "Text settings"; "base-style" -> "Background"
        "dimensions" -> "Dimensions"; else -> "Special effects"
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(12.dp), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(110.dp).background(Color(0xff080c13), RoundedCornerShape(12.dp))) {
                    val density = LocalDensity.current.density
                    val bitmap = remember(config, weather, maxWidth, density) {
                        PixelRenderer.render(config, weather, (maxWidth.value * density).roundToInt(), (110 * density).roundToInt())
                    }
                    Image(bitmap.asImageBitmap(), "Widget appearance preview", filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (page) {
                        "icon-style" -> {
                            Label("Theme")
                            ChoiceRow(IconTheme.entries.map { it.label }, a.theme.ordinal) { change(a.copy(theme = IconTheme.entries[it])) }
                            Label("Icon size")
                            ChoiceRow(listOf("Small", "Medium", "Large"), a.iconScale - 1) { change(a.copy(iconScale = it + 1)) }
                            NumberSlider("Icon padding", a.iconPadding, 0..8) { change(a.copy(iconPadding = it)) }
                            Alignments(a.iconHorizontal, a.iconVertical, { change(a.copy(iconHorizontal = it)) }, { change(a.copy(iconVertical = it)) })
                            HorizontalDivider()
                            ColorEditor("Icon", config.iconColor, a.icon) { color, treatment -> onChange(config.copy(iconColor = color, appearance = a.copy(icon = treatment))) }
                        }
                        "text-style" -> {
                            Label("Text size")
                            ChoiceRow(listOf("Small", "Medium", "Large", "Extra large"), a.textScale - 1) { change(a.copy(textScale = it + 1)) }
                            NumberSlider("Text padding", a.textPadding, 0..8) { change(a.copy(textPadding = it)) }
                            Alignments(a.textHorizontal, a.textVertical, { change(a.copy(textHorizontal = it)) }, { change(a.copy(textVertical = it)) })
                            HorizontalDivider()
                            ColorEditor("Text", config.textColor, a.text) { color, treatment -> onChange(config.copy(textColor = color, appearance = a.copy(text = treatment))) }
                        }
                        "base-style" -> {
                            Toggle("Transparent background", config.backgroundColor == 0) {
                                onChange(config.copy(backgroundColor = if (it) 0 else 0xff11151d.toInt()))
                            }
                            if (config.backgroundColor != 0) ColorEditor("Background", config.backgroundColor, a.background) { color, treatment ->
                                onChange(config.copy(backgroundColor = color, appearance = a.copy(background = treatment)))
                            } else Text("Your wallpaper shows behind the icon and text.", fontSize = 13.sp)
                        }
                        "dimensions" -> {
                            Toggle("Dynamic sizing", a.dynamicSizing) { change(a.copy(dynamicSizing = it)) }
                            Text("Fits the available home-screen space. Smaller widgets reduce the requested sizes when needed.", fontSize = 13.sp)
                            if (a.dynamicSizing) {
                                Label("Scale · ${(config.scale * 100).roundToInt()}%")
                                Slider(config.scale, { onChange(config.copy(scale = it)) }, valueRange = .4f..1f, steps = 5)
                            } else NumberSlider("Pixel size", a.fixedPixelSize, 1..12) { change(a.copy(fixedPixelSize = it)) }
                            NumberSlider("Outer padding", a.outerPadding, 0..32) { change(a.copy(outerPadding = it)) }
                            Alignments(a.horizontal, a.vertical, { change(a.copy(horizontal = it)) }, { change(a.copy(vertical = it)) })
                        }
                        "effects" -> {
                            Toggle("Contrast outlines", a.outlines) { change(a.copy(outlines = it)) }
                            Text("Adds a one-pixel border to keep the icon and text visible against your wallpaper.", fontSize = 13.sp)
                            if (a.outlines) Palette("Outline color", a.outlineColor) { change(a.copy(outlineColor = it)) }
                            HorizontalDivider()
                            Label("Content blend mode")
                            ChoiceRow(listOf("Normal", "Cutout"), if (a.cutout) 1 else 0) { change(a.copy(cutout = it == 1)) }
                            Text("Cutout makes the icon and text transparent so your wallpaper shows through.", fontSize = 13.sp)
                            if (a.cutout && (config.backgroundColor == 0 || a.background.opacity == 0) && !a.outlines)
                                Text("Choose a visible background or enable outlines to see the cutout. The preview uses normal content until then.", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                    if (page == "icon-style" || page == "text-style") {
                        Text("Alignment applies where the selected layout leaves extra space.", fontSize = 12.sp)
                        if (a.cutout) Text("Cutout uses transparent content; its colors and opacity apply again in Normal mode.", fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable private fun NumberSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Label("$label · $value")
    Slider(value.toFloat(), { onChange(it.roundToInt()) }, valueRange = range.first.toFloat()..range.last.toFloat(), steps = (range.last - range.first - 1).coerceAtLeast(0))
}

@Composable private fun Alignments(horizontal: Position, vertical: Position, onHorizontal: (Position) -> Unit, onVertical: (Position) -> Unit) {
    Label("Horizontal alignment")
    ChoiceRow(listOf("Left", "Center", "Right"), horizontal.ordinal) { onHorizontal(Position.entries[it]) }
    Label("Vertical alignment")
    ChoiceRow(listOf("Top", "Center", "Bottom"), vertical.ordinal) { onVertical(Position.entries[it]) }
}

@Composable private fun ColorEditor(label: String, base: Int, treatment: ColorTreatment, onChange: (Int, ColorTreatment) -> Unit) {
    var selected by rememberSaveable(label) { mutableIntStateOf(0) }
    val gradient = treatment.stops.isNotEmpty()
    Label("$label color")
    ChoiceRow(listOf("Solid color", "Gradient"), if (gradient) 1 else 0) {
        selected = 0
        onChange(base, treatment.copy(stops = if (it == 0) emptyList() else listOf(0xffed9ab4.toInt())))
    }
    val colors = listOf(base) + treatment.stops
    val index = selected.coerceIn(colors.indices)
    if (gradient) {
        ChoiceRow(colors.indices.map { "Color ${it + 1}" }, index) { selected = it }
    }
    Palette(if (gradient) "$label color ${index + 1}" else "$label color", colors[index]) { color ->
        if (index == 0) onChange(color, treatment)
        else onChange(base, treatment.copy(stops = treatment.stops.toMutableList().also { it[index - 1] = color }))
    }
    if (gradient) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (colors.size < 4) TextButton(onClick = {
                selected = colors.size
                onChange(base, treatment.copy(stops = treatment.stops + 0xff99d5bc.toInt()))
            }) { Text("Add color") }
            if (index > 0) TextButton(onClick = {
                val reordered = colors.toMutableList()
                val color = reordered.removeAt(index); reordered.add(index - 1, color)
                selected = index - 1
                onChange(reordered.first(), treatment.copy(stops = reordered.drop(1)))
            }) { Text("Move earlier") }
            if (colors.size > 2) TextButton(onClick = {
                val remaining = colors.toMutableList().also { it.removeAt(index) }
                selected = (index - 1).coerceAtLeast(0)
                onChange(remaining.first(), treatment.copy(stops = remaining.drop(1)))
            }) { Text("Remove color") }
        }
        Label("Gradient direction")
        ChoiceRow(GradientDirection.entries.map { it.label }, treatment.direction.ordinal) { onChange(base, treatment.copy(direction = GradientDirection.entries[it])) }
    }
    NumberSlider("$label opacity", treatment.opacity, 0..100) { onChange(base, treatment.copy(opacity = it)) }
}
