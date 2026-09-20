package com.retroweather.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LocationResult(val place: Place?, val message: String)

class DeviceLocation(private val context: Context, private val store: AppStore) {
    fun foregroundAllowed() = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun backgroundAllowed() = Build.VERSION.SDK_INT < 29 || context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    suspend fun resolve(background: Boolean): LocationResult {
        if (!foregroundAllowed()) return LocationResult(store.dynamicPlace(), "Location permission is off. Using the last location if available.")
        if (background && !backgroundAllowed()) return LocationResult(store.dynamicPlace(), "Open the app to update location, or allow location all the time.")
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = manager.getProviders(true).filter { it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER }
        if (providers.isEmpty()) return LocationResult(store.dynamicPlace(), "Device location is off. Using the last location if available.")
        val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time in 0..10 * 60_000L }.maxByOrNull { it.time }
        val fix = recent ?: withContext(Dispatchers.Main) {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            manager.removeUpdates(this)
                            if (continuation.isActive) continuation.resume(location)
                        }
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                        @Deprecated("Deprecated in Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    }
                    continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                    var registered = false
                    for (provider in providers) {
                        runCatching { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }.onSuccess { registered = true }
                    }
                    if (!registered && continuation.isActive) continuation.resume(null)
                }
            }
        }
        return if (fix != null) {
            val place = Place("Current location", fix.latitude, fix.longitude)
            store.saveDynamic(place, fix.time)
            LocationResult(place, "")
        } else LocationResult(store.dynamicPlace(), "Couldn't get a fresh location. Using the last location if available.")
    }
}
