package com.retroweather.data

import org.json.JSONArray
import org.json.JSONObject

enum class IconTheme(val label: String) { OUTLINE("Outline"), SOLID("Solid") }
enum class Position { START, CENTER, END;
    fun offset(space: Int): Int = when (this) { START -> 0; CENTER -> space / 2; END -> space }.coerceAtLeast(0)
}
enum class GradientDirection(val label: String) { HORIZONTAL("Across"), VERTICAL("Down"), DIAGONAL("Diagonal") }

/** Extra gradient stops follow the existing base color, keeping old saved widgets compatible. */
data class ColorTreatment(
    val stops: List<Int> = emptyList(),
    val opacity: Int = 100,
    val direction: GradientDirection = GradientDirection.DIAGONAL
) {
    fun json() = JSONObject().put("stops", JSONArray(stops)).put("opacity", opacity).put("direction", direction.name)
    companion object {
        fun from(j: JSONObject?) = if (j == null) ColorTreatment() else ColorTreatment(
            j.optJSONArray("stops")?.let { a -> (0 until minOf(a.length(), 3)).map { a.optInt(it, -1) } } ?: emptyList(),
            j.optInt("opacity", 100).coerceIn(0, 100), enumOr(j.optString("direction"), GradientDirection.DIAGONAL)
        )
    }
}

data class Appearance(
    val theme: IconTheme = IconTheme.OUTLINE,
    val iconScale: Int = 1,
    val textScale: Int = 2,
    val iconPadding: Int = 0,
    val textPadding: Int = 0,
    val outerPadding: Int = 4,
    val horizontal: Position = Position.CENTER,
    val vertical: Position = Position.CENTER,
    val iconHorizontal: Position = Position.CENTER,
    val iconVertical: Position = Position.CENTER,
    val textHorizontal: Position = Position.CENTER,
    val textVertical: Position = Position.CENTER,
    val dynamicSizing: Boolean = true,
    val fixedPixelSize: Int = 4,
    val icon: ColorTreatment = ColorTreatment(),
    val text: ColorTreatment = ColorTreatment(),
    val background: ColorTreatment = ColorTreatment(),
    val outlines: Boolean = false,
    val outlineColor: Int = 0xff11151d.toInt(),
    val cutout: Boolean = false
) {
    fun json() = JSONObject().put("theme", theme.name).put("iconScale", iconScale).put("textScale", textScale)
        .put("iconPadding", iconPadding).put("textPadding", textPadding).put("outerPadding", outerPadding)
        .put("horizontal", horizontal.name).put("vertical", vertical.name)
        .put("iconHorizontal", iconHorizontal.name).put("iconVertical", iconVertical.name)
        .put("textHorizontal", textHorizontal.name).put("textVertical", textVertical.name)
        .put("dynamicSizing", dynamicSizing).put("fixedPixelSize", fixedPixelSize)
        .put("icon", icon.json()).put("text", text.json()).put("background", background.json())
        .put("outlines", outlines).put("outlineColor", outlineColor).put("cutout", cutout)

    companion object {
        fun from(j: JSONObject?): Appearance {
            if (j == null) return Appearance()
            return Appearance(
                theme = enumOr(j.optString("theme"), IconTheme.OUTLINE),
                iconScale = j.optInt("iconScale", 1).coerceIn(1, 3), textScale = j.optInt("textScale", 2).coerceIn(1, 4),
                iconPadding = j.optInt("iconPadding", 0).coerceIn(0, 8), textPadding = j.optInt("textPadding", 0).coerceIn(0, 8),
                outerPadding = j.optInt("outerPadding", 4).coerceIn(0, 32),
                horizontal = enumOr(j.optString("horizontal"), Position.CENTER), vertical = enumOr(j.optString("vertical"), Position.CENTER),
                iconHorizontal = enumOr(j.optString("iconHorizontal"), Position.CENTER), iconVertical = enumOr(j.optString("iconVertical"), Position.CENTER),
                textHorizontal = enumOr(j.optString("textHorizontal"), Position.CENTER), textVertical = enumOr(j.optString("textVertical"), Position.CENTER),
                dynamicSizing = j.optBoolean("dynamicSizing", true), fixedPixelSize = j.optInt("fixedPixelSize", 4).coerceIn(1, 12),
                icon = ColorTreatment.from(j.optJSONObject("icon")), text = ColorTreatment.from(j.optJSONObject("text")),
                background = ColorTreatment.from(j.optJSONObject("background")), outlines = j.optBoolean("outlines"),
                outlineColor = j.optInt("outlineColor", 0xff11151d.toInt()), cutout = j.optBoolean("cutout")
            )
        }
    }
}

private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T = runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
