package com.retroweather.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class ApiResponse(val code: Int, val body: String, val retryAfterMillis: Long = 0)
fun interface Transport { fun get(url: String): ApiResponse }
class HttpTransport : Transport {
    override fun get(url: String): ApiResponse {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "RetroWeather/0.1 Android")
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { reader ->
                // Bound untrusted responses rather than exhausting memory on unexpected payloads.
                val buffer = CharArray(512_001)
                var count = 0
                while (count < buffer.size) {
                    val n = reader.read(buffer, count, buffer.size - count)
                    if (n < 0) break
                    count += n
                }
                if (count == buffer.size) throw ApiFailure("Weather response is too large")
                String(buffer, 0, count)
            } ?: ""
            ApiResponse(code, body, retryDelay(connection.getHeaderField("Retry-After")))
        } finally { connection.disconnect() }
    }
    private fun retryDelay(value: String?): Long {
        if (value == null) return 0
        value.toLongOrNull()?.let { return it.coerceIn(0, 86400) * 1000 }
        return runCatching { (ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis()).coerceIn(0, 86_400_000) }.getOrDefault(0)
    }
}
class ApiFailure(message: String, val retryMillis: Long = 0) : Exception(message)

class WeatherApi(private val transport: Transport = HttpTransport()) {
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun fetch(url: String): JSONObject {
        val response = transport.get(url)
        if (response.code !in 200..299) throw ApiFailure(when (response.code) {
            401, 403 -> "Provider rejected credentials or access"
            429 -> "Provider request limit reached"
            else -> "Weather provider unavailable (${response.code})"
        }, response.retryAfterMillis)
        return try { JSONObject(response.body) } catch (_: Exception) { throw ApiFailure("Invalid weather response") }
    }

    suspend fun current(place: Place, fallbackKey: String? = null): Weather = withContext(Dispatchers.IO) {
        val coords = place.key.split(',')
        val now = System.currentTimeMillis()
        if (fallbackKey == null) {
            val json = fetch("https://api.open-meteo.com/v1/forecast?latitude=${coords[0]}&longitude=${coords[1]}&current=temperature_2m,weather_code,is_day&timeformat=unixtime&forecast_days=1")
            parseOpenMeteo(json, place.key, now)
        } else {
            val json = fetch("https://api.weatherapi.com/v1/current.json?key=${encode(fallbackKey)}&q=${encode(place.key)}&aqi=no")
            parseWeatherApi(json, place.key, now)
        }
    }
    suspend fun search(query: String, fallbackKey: String? = null): List<Place> = withContext(Dispatchers.IO) {
        if (fallbackKey == null) {
            val json = fetch("https://geocoding-api.open-meteo.com/v1/search?name=${encode(query.trim())}&count=8&language=en&format=json")
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            (0 until results.length()).map { i -> results.getJSONObject(i).let { j ->
                Place(listOf(j.getString("name"), j.optString("admin1"), j.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(", "), j.getDouble("latitude"), j.getDouble("longitude"))
            } }
        } else {
            val response = transport.get("https://api.weatherapi.com/v1/search.json?key=${encode(fallbackKey)}&q=${encode(query.trim())}")
            if (response.code !in 200..299) throw ApiFailure("Location search unavailable", response.retryAfterMillis)
            val results = org.json.JSONArray(response.body)
            (0 until results.length()).map { i -> results.getJSONObject(i).let { j ->
                Place(listOf(j.getString("name"), j.optString("region"), j.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(", "), j.getDouble("lat"), j.getDouble("lon"))
            } }
        }
    }
    companion object {
        private fun validated(temp: Double, timestamp: Long, now: Long) {
            if (!temp.isFinite() || temp !in -100.0..70.0 || timestamp > now + 600_000 || now - timestamp > 7_200_000) throw ApiFailure("Provider returned stale or invalid weather")
        }
        fun parseOpenMeteo(json: JSONObject, key: String, now: Long): Weather {
            val current = json.getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val time = current.getLong("time") * 1000
            validated(temp, time, now)
            return Weather(temp, Conditions.openMeteo(current.getInt("weather_code")), current.getInt("is_day") == 1, time, now, "Open-Meteo", key)
        }
        fun parseWeatherApi(json: JSONObject, key: String, now: Long): Weather {
            val current = json.getJSONObject("current")
            val temp = current.getDouble("temp_c")
            val time = current.getLong("last_updated_epoch") * 1000
            validated(temp, time, now)
            return Weather(temp, Conditions.weatherApi(current.getJSONObject("condition").getInt("code")), current.getInt("is_day") == 1, time, now, "WeatherAPI.com", key)
        }
    }
}
