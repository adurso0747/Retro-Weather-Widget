package com.retroweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.work.*
import com.retroweather.MainActivity
import com.retroweather.R
import com.retroweather.art.PixelRenderer
import com.retroweather.data.*
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class WeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, it) }
        schedule(context)
        refresh(context)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { render(context, id) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { AppStore(context).deleteConfig(it) } }
    override fun onDisabled(context: Context) { WorkManager.getInstance(context).cancelUniqueWork("weather-periodic") }
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val store = AppStore(context)
        oldWidgetIds.zip(newWidgetIds).forEach { (old, new) ->
            if (store.hasConfig(old)) { store.saveConfig(new, store.config(old)); store.deleteConfig(old) }
        }
    }
    companion object {
        fun ids(context: Context) = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, WeatherWidget::class.java))
        fun schedule(context: Context) {
            if (ids(context).isEmpty()) return
            val work = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("weather-periodic", ExistingPeriodicWorkPolicy.KEEP, work)
        }
        fun refresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("weather-refresh", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<WeatherWorker>().build())
        }
        fun render(context: Context, id: Int) {
            val store = AppStore(context)
            val config = store.config(id)
            val manager = AppWidgetManager.getInstance(context)
            val options = manager.getAppWidgetOptions(id)
            val density = context.resources.displayMetrics.density
            val landscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val widthDp = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160).coerceIn(48, 400)
            val heightDp = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 80).coerceIn(32, 240)
            val weather = store.weather(store.resolved(config))
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            views.setImageViewBitmap(R.id.widget_image, PixelRenderer.render(config, weather, (widthDp * density).toInt(), (heightDp * density).toInt()))
            val message = when {
                !store.hasConfig(id) -> "Tap to set up Retro Weather"
                store.resolved(config) == null -> "Tap to choose a location"
                weather == null -> "Weather unavailable · tap to refresh"
                else -> ""
            }
            views.setTextViewText(R.id.widget_message, message)
            views.setViewVisibility(R.id.widget_message, if (message.isBlank()) View.GONE else View.VISIBLE)
            views.setContentDescription(R.id.widget_image, weather?.let {
                "${it.kind.label}, ${it.temperature(config.fahrenheit)}. ${store.resolved(config)?.name}. ${if (it.stale()) "Saved weather is over two hours old." else ""} ${store.status(id)}"
            } ?: message)
            val intent = Intent(context, MainActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra("widgetTap", message.isBlank()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, views)
        }
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = AppStore(applicationContext)
        val ids = WeatherWidget.ids(applicationContext)
        if (ids.isEmpty()) return Result.success()
        val dynamic = if (ids.any { store.hasConfig(it) && store.config(it).dynamic }) DeviceLocation(applicationContext, store).resolve(true) else null
        val results = mutableMapOf<String, RefreshResult>()
        for (id in ids) {
            if (!store.hasConfig(id)) continue
            val config = store.config(id)
            val place = if (config.dynamic) dynamic?.place else config.place
            try {
                val result = place?.let { results.getOrPutSuspend(it.key) { WeatherRepository(store).refresh(it) } }
                store.status(id, listOf(if (config.dynamic) dynamic?.message.orEmpty() else "", result?.message.orEmpty()).filter { it.isNotBlank() }.joinToString(" "))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { store.status(id, "Refresh failed. Showing saved weather if available.") }
            WeatherWidget.render(applicationContext, id)
        }
        return Result.success()
    }
    private suspend fun <K, V> MutableMap<K, V>.getOrPutSuspend(key: K, block: suspend () -> V): V {
        return get(key) ?: block().also { put(key, it) }
    }
}

class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || id !in WeatherWidget.ids(context)) return
        val token = intent.getStringExtra("pinToken") ?: return
        val config = AppStore(context).consumePin(token) ?: return
        AppStore(context).saveConfig(id, config)
        WeatherWidget.render(context, id)
        WeatherWidget.schedule(context)
        WeatherWidget.refresh(context)
    }
}
