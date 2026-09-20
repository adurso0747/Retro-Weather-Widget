package com.retroweather.data

import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

enum class WeatherKind(val label: String) {
    CLEAR("Clear"), PARTLY_CLOUDY("Partly cloudy"), CLOUDY("Overcast"), FOG("Fog"),
    DRIZZLE("Drizzle"), RAIN("Rain"), HEAVY_RAIN("Heavy rain"), FREEZING("Freezing rain"),
    SNOW("Snow"), HEAVY_SNOW("Heavy snow"), SHOWERS("Showers"), SNOW_SHOWERS("Snow showers"),
    THUNDER("Thunderstorm"), HAIL("Thunderstorm with hail"), UNKNOWN("Unavailable")
}

object Conditions {
    fun openMeteo(code: Int): WeatherKind = when (code) {
        0 -> WeatherKind.CLEAR
        1, 2 -> WeatherKind.PARTLY_CLOUDY
        3 -> WeatherKind.CLOUDY
        45, 48 -> WeatherKind.FOG
        51, 53, 55 -> WeatherKind.DRIZZLE
        56, 57, 66, 67 -> WeatherKind.FREEZING
        61, 63 -> WeatherKind.RAIN
        65 -> WeatherKind.HEAVY_RAIN
        71, 73, 77 -> WeatherKind.SNOW
        75 -> WeatherKind.HEAVY_SNOW
        80, 81, 82 -> WeatherKind.SHOWERS
        85, 86 -> WeatherKind.SNOW_SHOWERS
        95 -> WeatherKind.THUNDER
        96, 99 -> WeatherKind.HAIL
        else -> WeatherKind.UNKNOWN
    }

    fun weatherApi(code: Int): WeatherKind = when (code) {
        1000 -> WeatherKind.CLEAR
        1003 -> WeatherKind.PARTLY_CLOUDY
        1006, 1009 -> WeatherKind.CLOUDY
        1030, 1135, 1147 -> WeatherKind.FOG
        1150, 1153 -> WeatherKind.DRIZZLE
        1063, 1180, 1183, 1186, 1189 -> WeatherKind.RAIN
        1192, 1195 -> WeatherKind.HEAVY_RAIN
        1069, 1072, 1168, 1171, 1198, 1201, 1204, 1207, 1237, 1249, 1252, 1261, 1264 -> WeatherKind.FREEZING
        1066, 1210, 1213, 1216, 1219 -> WeatherKind.SNOW
        1114, 1117, 1222, 1225 -> WeatherKind.HEAVY_SNOW
        1240, 1243, 1246 -> WeatherKind.SHOWERS
        1255, 1258 -> WeatherKind.SNOW_SHOWERS
        1087, 1273, 1276, 1279, 1282 -> WeatherKind.THUNDER
        else -> WeatherKind.UNKNOWN
    }
}

data class Place(val name: String, val latitude: Double, val longitude: Double) {
    init { require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0) }
    // Approximately 1 km precision: adequate for weather, reduces coordinate disclosure and cache churn.
    val key: String get() = String.format(Locale.US, "%.2f,%.2f", latitude, longitude)
    fun json() = JSONObject().put("name", name).put("lat", latitude).put("lon", longitude)
    companion object { fun from(j: JSONObject) = Place(j.getString("name"), j.getDouble("lat"), j.getDouble("lon")) }
}

enum class Layout(val label: String) { LEFT("Left"), TOP("Top"), RIGHT("Right"), BOTTOM("Bottom") }

data class WidgetConfig(
    val dynamic: Boolean = false,
    val place: Place? = null,
    val fahrenheit: Boolean = true,
    val layout: Layout = Layout.LEFT,
    val iconColor: Int = -1,
    val textColor: Int = -1,
    val backgroundColor: Int = 0,
    val showIcon: Boolean = true,
    val showTemperature: Boolean = true,
    val scale: Float = 0.85f,
    val shortcutPackage: String = "",
    val shortcutLabel: String = "Retro Weather"
) {
    fun json(): JSONObject = JSONObject().put("dynamic", dynamic).put("place", place?.json())
        .put("fahrenheit", fahrenheit).put("layout", layout.name).put("iconColor", iconColor)
        .put("textColor", textColor).put("backgroundColor", backgroundColor).put("showIcon", showIcon)
        .put("showTemperature", showTemperature).put("scale", scale.toDouble())
        .put("shortcutPackage", shortcutPackage).put("shortcutLabel", shortcutLabel)
    companion object {
        fun from(j: JSONObject) = WidgetConfig(
            j.optBoolean("dynamic"), j.optJSONObject("place")?.let(Place::from), j.optBoolean("fahrenheit", true),
            runCatching { Layout.valueOf(j.optString("layout")) }.getOrDefault(Layout.LEFT),
            j.optInt("iconColor", -1), j.optInt("textColor", -1), j.optInt("backgroundColor", 0),
            j.optBoolean("showIcon", true), j.optBoolean("showTemperature", true),
            j.optDouble("scale", 0.85).toFloat().coerceIn(0.4f, 1f),
            j.optString("shortcutPackage"), j.optString("shortcutLabel", "Retro Weather")
        )
    }
}

data class Weather(
    val celsius: Double, val kind: WeatherKind, val isDay: Boolean,
    val observedAt: Long, val fetchedAt: Long, val source: String, val placeKey: String
) {
    fun temperature(fahrenheit: Boolean): String =
        "${(if (fahrenheit) celsius * 9 / 5 + 32 else celsius).roundToInt()}°${if (fahrenheit) "F" else "C"}"
    fun stale(now: Long = System.currentTimeMillis()) = now - observedAt > 2 * 60 * 60 * 1000L
    fun json() = JSONObject().put("celsius", celsius).put("kind", kind.name).put("isDay", isDay)
        .put("observedAt", observedAt).put("fetchedAt", fetchedAt).put("source", source).put("placeKey", placeKey)
    companion object {
        fun from(j: JSONObject) = Weather(j.getDouble("celsius"), WeatherKind.valueOf(j.getString("kind")),
            j.getBoolean("isDay"), j.getLong("observedAt"), j.getLong("fetchedAt"), j.getString("source"), j.getString("placeKey"))
    }
}
