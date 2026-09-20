package com.retroweather.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small, atomic preference records. Credentials are encrypted separately with Android Keystore. */
class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("retro_weather", Context.MODE_PRIVATE)
    private fun json(key: String): JSONObject? = prefs.getString(key, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
    fun config(id: Int = 0): WidgetConfig = json("widget_$id")?.let { runCatching { WidgetConfig.from(it) }.getOrNull() } ?: WidgetConfig()
    fun hasConfig(id: Int) = prefs.contains("widget_$id")
    fun saveConfig(id: Int, config: WidgetConfig) { prefs.edit().putString("widget_$id", config.json().toString()).apply() }
    fun deleteConfig(id: Int) { prefs.edit().remove("widget_$id").remove("status_$id").apply() }
    fun weather(place: Place?): Weather? = place?.let { json("weather_${it.key}") }?.let { runCatching { Weather.from(it) }.getOrNull() }
    fun saveWeather(weather: Weather) {
        // Only a small recent cache is retained; location history is not accumulated indefinitely.
        val existing = prefs.all.filterKeys { it.startsWith("weather_") }
        val edit = prefs.edit()
        if (existing.size >= 20) existing.keys.filter { it != "weather_${weather.placeKey}" }
            .sortedBy { json(it)?.optLong("fetchedAt", 0) ?: 0 }.take(existing.size - 19).forEach(edit::remove)
        edit.putString("weather_${weather.placeKey}", weather.json().toString()).apply()
    }
    fun status(id: Int) = prefs.getString("status_$id", "") ?: ""
    fun status(id: Int, message: String) { prefs.edit().putString("status_$id", message).apply() }
    fun dynamicPlace(): Place? = json("dynamic_place")?.let { runCatching { Place.from(it) }.getOrNull() }
    fun dynamicTime() = prefs.getLong("dynamic_time", 0)
    fun saveDynamic(place: Place, timestamp: Long) { prefs.edit().putString("dynamic_place", place.json().toString()).putLong("dynamic_time", timestamp).apply() }
    fun resolved(config: WidgetConfig): Place? = if (config.dynamic) dynamicPlace() else config.place
    fun cooldown(provider: String) = prefs.getLong("cooldown_$provider", 0)
    fun cooldown(provider: String, until: Long) { prefs.edit().putLong("cooldown_$provider", until).apply() }
    fun lastAttempt(key: String) = prefs.getLong("attempt_$key", 0)
    fun lastAttempt(key: String, time: Long) {
        val attempts = prefs.all.filterKeys { it.startsWith("attempt_") }
        val edit = prefs.edit()
        if (attempts.size >= 20) attempts.keys.filter { it != "attempt_$key" }
            .sortedBy { prefs.getLong(it, 0) }.take(attempts.size - 19).forEach(edit::remove)
        edit.putLong("attempt_$key", time).apply()
    }
    fun savePin(token: String, config: WidgetConfig) {
        val edit = prefs.edit()
        // Only one launcher confirmation is active at a time; discard abandoned requests.
        prefs.all.keys.filter { it.startsWith("pin_") }.forEach(edit::remove)
        edit.putString("pin_$token", config.json().toString()).apply()
    }
    fun consumePin(token: String): WidgetConfig? {
        val config = json("pin_$token")?.let(WidgetConfig::from)
        prefs.edit().remove("pin_$token").apply()
        return config
    }
    fun hasFallbackKey() = prefs.contains("fallback_key")
    fun fallbackKey(): String = runCatching {
        val stored = prefs.getString("fallback_key", null) ?: return ""
        val parts = stored.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrDefault("")
    fun saveFallbackKey(value: String) {
        if (value.isBlank()) { prefs.edit().remove("fallback_key").apply(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        val encrypted = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("fallback_key", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
        cooldown("fallback", 0)
    }
    private fun secret(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("retro_weather_api", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("retro_weather_api", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
