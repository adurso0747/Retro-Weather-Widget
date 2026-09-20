package com.retroweather.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RefreshResult(val weather: Weather?, val message: String)

/** Serializes refreshes across the activity and worker, coalescing widgets at the same location. */
class WeatherRepository(private val store: AppStore, private val api: WeatherApi = WeatherApi()) {
    suspend fun refresh(place: Place, manual: Boolean = false): RefreshResult = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = store.weather(place)
        if (cached != null && !cached.stale(now) && now - cached.fetchedAt < if (manual) 60_000 else 25 * 60_000)
            return@withLock RefreshResult(cached, "")
        if (now - store.lastAttempt(place.key) < 60_000) return@withLock RefreshResult(cached, "Please wait a minute before refreshing again.")
        store.lastAttempt(place.key, now)
        var reason = "Primary provider is cooling down."
        if (now >= store.cooldown("primary")) {
            try {
                val fresh = api.current(place)
                store.saveWeather(fresh)
                store.cooldown("primary", 0)
                return@withLock RefreshResult(fresh, "")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                reason = if (e is ApiFailure) e.message ?: "Primary weather unavailable." else "Cannot reach Open-Meteo."
                store.cooldown("primary", now + maxOf(5 * 60_000L, (e as? ApiFailure)?.retryMillis ?: 0))
            }
        }
        val key = store.fallbackKey()
        if (key.isNotBlank() && now >= store.cooldown("fallback")) {
            try {
                val fresh = api.current(place, key)
                store.saveWeather(fresh)
                return@withLock RefreshResult(fresh, "Using fallback weather. Open-Meteo will be retried automatically.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                store.cooldown("fallback", now + maxOf(5 * 60_000L, (e as? ApiFailure)?.retryMillis ?: 0))
                reason += if (e is ApiFailure) " Fallback: ${e.message}." else " Cannot reach fallback provider."
            }
        }
        RefreshResult(cached, "$reason ${if (cached != null) "Showing saved weather." else "No saved weather for this location."}")
    }
    suspend fun search(query: String): List<Place> {
        try { return api.search(query) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            val key = store.fallbackKey()
            if (key.isBlank()) throw ApiFailure("Location search unavailable. Try coordinates or search again later.")
            return api.search(query, key)
        }
    }
    companion object { private val mutex = Mutex() }
}
