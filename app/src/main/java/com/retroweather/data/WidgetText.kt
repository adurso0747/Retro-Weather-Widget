package com.retroweather.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A bounded template language: substitutions are data and are never parsed a second time. */
object WidgetText {
    const val DEFAULT = "{temperature:unit}"
    val tokens = listOf("temperature:unit", "temperature", "unit", "condition", "location", "updated", "date")
    private val placeholder = Regex("\\{([^{}]+)\\}")

    fun error(template: String): String? {
        if (template.isBlank()) return "Enter text or a weather field."
        if (template.length > 160) return "Use 160 characters or fewer."
        if (template.count { it == '\n' } > 3) return "Use four lines or fewer."
        if (template.any { it.isISOControl() && it != '\n' }) return "Only line breaks are supported as control characters."
        val unknown = placeholder.findAll(template).firstOrNull { it.groupValues[1] !in tokens }
        if (unknown != null) return "Unknown field: ${unknown.value}"
        if (placeholder.replace(template, "").any { it == '{' || it == '}' }) return "Close each field with matching braces."
        return null
    }

    fun resolve(config: WidgetConfig, weather: Weather?, zone: ZoneId = ZoneId.systemDefault()): String {
        val template = config.textTemplate.takeIf { error(it) == null } ?: DEFAULT
        val unit = if (config.fahrenheit) "°F" else "°C"
        fun time(pattern: String) = weather?.let {
            DateTimeFormatter.ofPattern(pattern, Locale.ROOT).withZone(zone).format(Instant.ofEpochMilli(it.observedAt))
        } ?: "--"
        val values = mapOf(
            "temperature:unit" to (weather?.temperature(config.fahrenheit) ?: "--$unit"),
            "temperature" to (weather?.temperature(config.fahrenheit)?.removeSuffix(unit) ?: "--"),
            "unit" to unit,
            "condition" to (weather?.kind?.label ?: "Unavailable"),
            "location" to (if (config.dynamic) "Current location" else config.place?.name ?: "No location"),
            "updated" to time("HH:mm"), "date" to time("yyyy-MM-dd")
        )
        return placeholder.replace(template) { values.getValue(it.groupValues[1]) }
    }
}
