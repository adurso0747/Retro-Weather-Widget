package com.retroweather

import androidx.compose.ui.platform.testTag

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.retroweather.art.PixelArt
import com.retroweather.art.PixelRenderer
import com.retroweather.data.*
import com.retroweather.widget.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private val Ink = Color(0xff11151d)
private val Panel = Color(0xff1d2430)
private val Cream = Color(0xfff0ebde)
private val Amber = Color(0xfff4be65)
private val Muted = Color(0xffb2bac7)
private val Mint = Color(0xff99d5bc)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val store = AppStore(this)
        val requestedId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        val id = requestedId.takeIf { it in WeatherWidget.ids(this) } ?: 0
        var notice = ""
        if (savedInstanceState == null && intent.getBooleanExtra("widgetTap", false) && id != 0) {
            val config = store.config(id)
            if (config.shortcutPackage.isNotBlank()) {
                val launch = packageManager.getLaunchIntentForPackage(config.shortcutPackage)
                if (launch != null && runCatching { startActivity(launch) }.isSuccess) { finish(); return }
                notice = "${config.shortcutLabel} is unavailable. Choose another shortcut below."
            }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Amber, onPrimary = Ink, background = Ink,
                surface = Panel, onSurface = Cream, onBackground = Cream, secondary = Mint)) {
                Surface(Modifier.fillMaxSize(), color = Ink) { WeatherScreen(id, notice) }
            }
        }
    }

    @Composable private fun WeatherScreen(initialId: Int, notice: String) {
        val store = remember { AppStore(this) }
        val location = remember { DeviceLocation(this, store) }
        val repository = remember { WeatherRepository(store) }
        val scope = rememberCoroutineScope()
        var widgetId by rememberSaveable { mutableIntStateOf(initialId) }
        var savedJson by rememberSaveable(widgetId) { mutableStateOf(store.config(if (store.hasConfig(widgetId)) widgetId else 0).json().toString()) }
        val config = remember(savedJson) { WidgetConfig.from(JSONObject(savedJson)) }
        var shortcutNotice by rememberSaveable { mutableStateOf(notice) }
        fun change(next: WidgetConfig) {
            if (next.shortcutPackage != config.shortcutPackage) shortcutNotice = ""
            savedJson = next.json().toString()
        }
        fun changeAppearance(next: Appearance) { change(config.copy(appearance = next)) }
        var weather by remember { mutableStateOf(store.weather(store.resolved(config))) }
        var message by remember { mutableStateOf(store.status(widgetId)) }
        var busy by remember { mutableStateOf(false) }
        var refreshJob by remember { mutableStateOf<Job?>(null) }
        var permissionRevision by remember { mutableIntStateOf(0) }
        var widgetIds by remember { mutableStateOf(WeatherWidget.ids(this).toList()) }
        var dialog by remember { mutableStateOf("") }
        var query by rememberSaveable { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Place>>(emptyList()) }
        var searchBusy by remember { mutableStateOf(false) }
        var searchMessage by remember { mutableStateOf("") }
        var keyInput by remember { mutableStateOf("") }
        var keyPresent by remember { mutableStateOf(store.hasFallbackKey()) }
        var shortcutApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        var appQuery by remember { mutableStateOf("") }
        var latitude by rememberSaveable { mutableStateOf("") }
        var longitude by rememberSaveable { mutableStateOf("") }
        var coordinateError by remember { mutableStateOf("") }
        val currentConfig by rememberUpdatedState(config)

        fun refresh() {
            if (busy) return
            val start = config
            busy = true
            refreshJob = scope.launch {
                try {
                    val resolved = if (start.dynamic) location.resolve(false) else LocationResult(start.place, "")
                    if (resolved.place == null) {
                        message = resolved.message.ifBlank { "Choose a location to load your weather." }
                        weather = null
                    } else {
                        val result = repository.refresh(resolved.place, manual = true)
                        if (currentConfig.dynamic == start.dynamic && currentConfig.place == start.place) {
                            weather = result.weather
                            message = listOf(resolved.message, result.message).filter { it.isNotBlank() }.joinToString(" ")
                        }
                    }
                    WeatherWidget.ids(this@MainActivity).forEach { WeatherWidget.render(this@MainActivity, it) }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { message = "Refresh failed. Check your connection and try again." }
                finally { busy = false }
            }
        }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionRevision++
            if (location.foregroundAllowed()) refresh() else message = "Location permission was declined. You can still choose a fixed location."
        }
        val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRevision++ }
        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
                permissionRevision++
                widgetIds = WeatherWidget.ids(this@MainActivity).toList()
            } }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(widgetId, config.dynamic, config.place) {
            weather = store.weather(store.resolved(config))
            refreshJob?.cancelAndJoin()
            busy = false
            if (store.resolved(config) != null || (config.dynamic && location.foregroundAllowed())) refresh()
        }
        LaunchedEffect(permissionRevision) {
            // Returning from settings or recents also refreshes foreground-only device location.
            if (permissionRevision > 1 && !busy) {
                weather = store.weather(store.resolved(config))
                if (store.resolved(config) != null || (config.dynamic && location.foregroundAllowed())) refresh()
            }
        }

        fun save(): Boolean {
            if (!config.showIcon && !config.showTemperature) { message = "Keep the icon or temperature visible."; return false }
            if (store.resolved(config) == null) { message = "Choose a fixed location or resolve your current location first."; return false }
            store.saveConfig(widgetId, config)
            if (widgetId != 0) {
                WeatherWidget.render(this, widgetId)
                WeatherWidget.schedule(this)
                WeatherWidget.refresh(this)
            }
            message = "Widget settings saved."
            if (intent.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE && widgetId == initialId && initialId != 0) {
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            }
            return true
        }
        fun pin() {
            if (!save()) return
            val manager = AppWidgetManager.getInstance(this)
            if (!manager.isRequestPinAppWidgetSupported) {
                message = "Long-press your home screen, choose Widgets, then Retro Weather. Your saved default settings will be available."
                return
            }
            val token = UUID.randomUUID().toString()
            store.savePin(token, config)
            val callback = PendingIntent.getBroadcast(this, token.hashCode(), Intent(this, PinReceiver::class.java).putExtra("pinToken", token),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            val accepted = manager.requestPinAppWidget(ComponentName(this, WeatherWidget::class.java), null, callback)
            message = if (accepted) "Confirm placement on your home screen." else "Use your launcher's Widgets menu to add Retro Weather."
        }

        Scaffold(containerColor = Ink, bottomBar = {
            Surface(color = Ink, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save() }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Save widget", fontWeight = FontWeight.Bold) }
                    if (intent.action != AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        OutlinedButton(onClick = { pin() }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Add to home screen", fontSize = 12.sp) }
                }
            }
        }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).testTag("configuration-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RW", color = Amber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Text("RETRO WEATHER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(26.dp))
            Text("Configuration", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Spacer(Modifier.height(22.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xff080c13)), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("●  WIDGET PREVIEW", color = Mint, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Text(if (weather == null) "NO DATA" else if (weather!!.stale()) "SAVED" else "CURRENT", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    val density = resources.displayMetrics.density
                    val preview = remember(config, weather, maxWidth) { PixelRenderer.render(config, weather, (maxWidth.value * density).toInt(), (130 * density).toInt()) }
                    Image(preview.asImageBitmap(), weather?.let { "${it.kind.label}, ${it.temperature(config.fahrenheit)}" } ?: "Preview awaiting weather", filterQuality = FilterQuality.None, modifier = Modifier.fillMaxWidth().height(130.dp))
                }
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (config.dynamic) "Following your location" else config.place?.name ?: "Choose your location", fontSize = 12.sp, color = Cream, maxLines = 2)
                        Text(weather?.let { "${it.source} · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.observedAt))}" } ?: "Weather appears after setup", color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = { refresh() }, enabled = !busy) { Text(if (busy) "Loading…" else "Refresh", color = Amber) }
                }
            }
            if (shortcutNotice.isNotBlank()) Text(shortcutNotice, color = Amber, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            if (message.isNotBlank()) Text(message, color = Amber, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            Spacer(Modifier.height(16.dp))
            if (widgetIds.isNotEmpty()) Section("YOUR WIDGETS", "Edit each widget independently") {
                ChoiceRow(listOf("New widget") + widgetIds.map { "Widget $it" }, (listOf(0) + widgetIds).indexOf(widgetId)) {
                    widgetId = (listOf(0) + widgetIds)[it]
                    message = ""
                }
            }
            Section("01 / SETUP", "Location") {
                ChoiceRow(listOf("Fixed location", "Follow device"), if (config.dynamic) 1 else 0) { change(config.copy(dynamic = it == 1)); message = "" }
                if (config.dynamic) {
                    Text("Uses GPS or network location. Approximate location is enough. Coordinates are sent to your weather provider.", color = Muted, fontSize = 13.sp)
                    val allowed = remember(permissionRevision) { location.foregroundAllowed() }
                    val background = remember(permissionRevision) { location.backgroundAllowed() }
                    Text(if (allowed && background) "● Background location enabled" else if (allowed) "● Location updates when this app is open" else "○ Location permission needed", color = Mint, fontSize = 12.sp)
                    Button(onClick = {
                        if (!allowed) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) else refresh()
                    }, enabled = !busy) { Text(if (allowed) "Find my location" else "Allow device location") }
                    TextButton(onClick = { dialog = "background" }) { Text("Background location & permissions") }
                    store.dynamicPlace()?.let { Text("Last position: ${it.key}\n${DateFormat.getDateTimeInstance().format(Date(store.dynamicTime()))}", color = Muted, fontSize = 11.sp) }
                } else {
                    Text(config.place?.name ?: "Search for a city or enter coordinates.", color = Muted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dialog = "search" }) { Text("Find a city") }
                        TextButton(onClick = { dialog = "coordinates" }) { Text("Coordinates") }
                    }
                }
                store.resolved(config)?.let { place ->
                    TextButton(onClick = {
                        val map = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${place.key}?q=${place.key}(${Uri.encode(place.name)})"))
                        if (runCatching { startActivity(map) }.isFailure)
                            openUrl("https://www.openstreetmap.org/?mlat=${place.latitude}&mlon=${place.longitude}#map=12/${place.latitude}/${place.longitude}")
                    }) { Text("Open on map") }
                }
            }
            Section("02 / STYLE", "Appearance") {
                Label("Temperature")
                ChoiceRow(listOf("Fahrenheit  °F", "Celsius  °C"), if (config.fahrenheit) 0 else 1) { change(config.copy(fahrenheit = it == 0)) }
                Label("Icon position")
                ChoiceRow(Layout.entries.map { it.label }, config.layout.ordinal) { change(config.copy(layout = Layout.entries[it])) }
                Label("Icon theme")
                ChoiceRow(IconTheme.entries.map { it.label }, config.appearance.theme.ordinal) { changeAppearance(config.appearance.copy(theme = IconTheme.entries[it])) }
                Toggle("Weather icon", config.showIcon) { if (it || config.showTemperature) change(config.copy(showIcon = it)) }
                Toggle("Temperature", config.showTemperature) { if (it || config.showIcon) change(config.copy(showTemperature = it)) }
                Label("Scale · ${(config.scale * 100).toInt()}%")
                Slider(config.scale, onValueChange = { change(config.copy(scale = it)) }, valueRange = 0.4f..1f, steps = 5)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { dialog = "icon-style" }) { Text("Icon settings") }
                    OutlinedButton(onClick = { dialog = "text-style" }) { Text("Text settings") }
                    OutlinedButton(onClick = { dialog = "base-style" }) { Text("Background") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { dialog = "dimensions" }) { Text("Dimensions") }
                    TextButton(onClick = { dialog = "effects" }) { Text("Special effects") }
                }
                TextButton(onClick = { dialog = "gallery" }) { Text("Weather icons") }
            }
            Section("03 / ACTION", "Tap shortcut") {
                Text(config.shortcutLabel, color = Mint, fontWeight = FontWeight.Bold)
                Text("Tapping this widget opens the app you choose. You can always edit widgets here in Retro Weather.", color = Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        dialog = "apps"
                        scope.launch {
                            shortcutApps = withContext(Dispatchers.IO) {
                                packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
                                    .filter { it.activityInfo.packageName != packageName }
                                    .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
                            }
                        }
                    }) { Text("Choose app") }
                    if (config.shortcutPackage.isNotBlank()) TextButton(onClick = { change(config.copy(shortcutPackage = "", shortcutLabel = "Retro Weather")) }) { Text("Clear") }
                }
            }
            Section("04 / DATA", "Weather providers") {
                Text("Open-Meteo", fontWeight = FontWeight.Bold, color = Mint)
                Text("Refreshes about every 30 minutes while widgets are active. Android may delay updates to save battery. Saved weather stays available offline; an amber corner dot means data is over two hours old.", color = Muted, fontSize = 13.sp)
                HorizontalDivider(color = Color(0xff35404f))
                Text("WeatherAPI.com fallback", fontWeight = FontWeight.Bold)
                Text(if (keyPresent) "● API key saved securely on this device" else "Optional · add your free WeatherAPI.com key to switch providers during an outage.", color = Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { keyInput = ""; dialog = "key" }) { Text(if (keyPresent) "Replace key" else "Add API key") }
                    if (keyPresent) TextButton(onClick = { store.saveFallbackKey(""); keyPresent = false }) { Text("Remove") }
                }
                TextButton(onClick = { dialog = "about" }) { Text("Data sources & privacy") }
            }
            Spacer(Modifier.height(26.dp))
        }
        }

        when (dialog) {
            "icon-style", "text-style", "base-style", "dimensions", "effects" ->
                AppearanceEditor(dialog, config, weather, onChange = { change(it) }, onDismiss = { dialog = "" })
            "search" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Find your location") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(query, { query = it }, label = { Text("City or postal code") }, singleLine = true)
                    Button(enabled = query.trim().length >= 2 && !searchBusy, onClick = {
                        searchBusy = true; searchMessage = ""
                        scope.launch {
                            try { results = repository.search(query); if (results.isEmpty()) searchMessage = "No matches. Try a nearby city or coordinates." }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { searchMessage = "Search unavailable. Try again or enter coordinates." }
                            finally { searchBusy = false }
                        }
                    }) { Text(if (searchBusy) "Searching…" else "Search") }
                    if (searchMessage.isNotBlank()) Text(searchMessage, color = Amber)
                    results.forEach { place -> TextButton(onClick = { change(config.copy(dynamic = false, place = place)); dialog = ""; message = "" }) { Text(place.name) } }
                }
            }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
            "coordinates" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Enter coordinates") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude (−90 to 90)") }, singleLine = true)
                    OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude (−180 to 180)") }, singleLine = true)
                    if (coordinateError.isNotBlank()) Text(coordinateError, color = Amber)
                }
            }, confirmButton = { TextButton(onClick = {
                val place = runCatching { Place("Custom location", latitude.trim().toDouble(), longitude.trim().toDouble()) }.getOrNull()
                if (place == null) coordinateError = "Enter valid numeric coordinates." else { change(config.copy(dynamic = false, place = place)); dialog = ""; message = "" }
            }) { Text("Use location") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } })
            "background" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Follow your location") }, text = {
                Text("To follow you while the app is closed, allow location all the time in Android settings. We request occasional fixes, not continuous GPS tracking. You can decline and keep using the last location, or choose a fixed city.\n\nApproximate location is supported. If permissions were denied, you can also restore them here.")
            }, confirmButton = { TextButton(onClick = {
                dialog = ""
                if (Build.VERSION.SDK_INT == 29 && location.foregroundAllowed()) backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }) { Text("Open permissions") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Not now") } })
            "apps" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Choose tap shortcut") }, text = {
                Column {
                    OutlinedTextField(appQuery, { appQuery = it }, label = { Text("Filter apps") }, singleLine = true)
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        if (shortcutApps.isEmpty()) Text("No other launchable apps found.", modifier = Modifier.padding(16.dp))
                        shortcutApps.filter { it.second.contains(appQuery, true) }.forEach { (pkg, label) ->
                            TextButton(onClick = { change(config.copy(shortcutPackage = pkg, shortcutLabel = label)); dialog = "" }, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } })
            "key" -> AlertDialog(onDismissRequest = { keyInput = ""; dialog = "" }, title = { Text("Fallback API key") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create a free account at WeatherAPI.com, then paste its API key. The key is encrypted and stored on this device.")
                    OutlinedTextField(keyInput, { keyInput = it }, label = { Text("API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    TextButton(onClick = { openUrl("https://www.weatherapi.com/signup.aspx") }) { Text("Get a free key ↗") }
                }
            }, confirmButton = { TextButton(enabled = keyInput.isNotBlank(), onClick = {
                runCatching { store.saveFallbackKey(keyInput) }.onSuccess { keyPresent = true; message = "Fallback key saved. It will be used when Open-Meteo is unavailable." }.onFailure { message = "Couldn't securely store the key. Please try again." }
                keyInput = ""; dialog = ""
            }) { Text("Save key") } }, dismissButton = { TextButton(onClick = { keyInput = ""; dialog = "" }) { Text("Cancel") } })
            "about" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Data sources & privacy") }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Weather data by Open-Meteo, licensed CC BY 4.0. Values are rounded and condition codes are translated into original pixel artwork. Optional fallback data by WeatherAPI.com.")
                    Text("Only the latest device position and up to 20 recent weather cache entries are kept locally. No travel log, analytics, ads, or account is built into this app. Android backup is disabled. Uninstalling removes local settings.")
                    Text("Weather requests send coordinates rounded to about 1 km and your IP address to the active provider. City searches send the search text. Providers may keep server logs. Background location is optional. Free provider access has no uptime guarantee.")
                    TextButton(onClick = { openUrl("https://open-meteo.com/") }) { Text("Open-Meteo ↗") }
                    TextButton(onClick = { openUrl("https://creativecommons.org/licenses/by/4.0/") }) { Text("CC BY 4.0 license ↗") }
                    TextButton(onClick = { openUrl("https://open-meteo.com/en/terms") }) { Text("Open-Meteo terms & privacy ↗") }
                    TextButton(onClick = { openUrl("https://www.weatherapi.com/privacy.aspx") }) { Text("WeatherAPI.com privacy ↗") }
                }
            }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
            "gallery" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Weather icons") }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceRow(IconTheme.entries.map { it.label }, config.appearance.theme.ordinal) { changeAppearance(config.appearance.copy(theme = IconTheme.entries[it])) }
                    Text("Original 32 × 32 grids. Day and night variants below.", color = Muted)
                    WeatherKind.entries.forEach { kind ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(true, false).forEach { day ->
                                val sample = Weather(18.0, kind, day, System.currentTimeMillis(), System.currentTimeMillis(), "Preview", "")
                                val bitmap = remember(kind, day, config.appearance.theme) { PixelRenderer.render(WidgetConfig(showTemperature = false, iconColor = 0xfff4be65.toInt(), appearance = Appearance(theme = config.appearance.theme)), sample, 96, 96) }
                                Image(bitmap.asImageBitmap(), "${kind.label}, ${if (day) "day" else "night"}", filterQuality = FilterQuality.None, modifier = Modifier.size(52.dp))
                            }
                            Text(kind.label, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
        }
    }
    private fun openUrl(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
}

@Composable private fun Section(kicker: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(kicker, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            content()
        }
    }
}
@Composable internal fun Label(text: String) { Text(text, color = Muted, fontSize = 12.sp) }
@Composable internal fun ChoiceRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        labels.forEachIndexed { i, label -> FilterChip(selected = selected == i, onClick = { onSelect(i) }, label = { Text(label, fontSize = 12.sp) }) }
    }
}
@Composable internal fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f)); Switch(checked, onCheckedChange = onChange)
    }
}
@Composable internal fun Palette(label: String, selected: Int, onSelect: (Int) -> Unit) {
    val colors = listOf(0xfff0ebde, 0xffffffff, 0xfff4be65, 0xff99d5bc, 0xffa6bdff, 0xffed9ab4, 0xff11151d).map { it.toInt() }
    var showCustom by remember { mutableStateOf(false) }
    var hex by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Label(label)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEach { color ->
            Box(Modifier.size(34.dp).border(if (color == selected) 2.dp else 1.dp, if (color == selected) Amber else Color(0xff4a5361), RoundedCornerShape(8.dp))
                .padding(4.dp).background(Color(color), RoundedCornerShape(4.dp)).semantics { contentDescription = "$label #%06X%s".format(color and 0xffffff, if (color == selected) ", selected" else "") }.clickable { onSelect(color) })
        }
        TextButton(onClick = { hex = "%06X".format(selected and 0xffffff); error = false; showCustom = true }) { Text("Hex") }
    }
    if (showCustom) AlertDialog(onDismissRequest = { showCustom = false }, title = { Text(label) }, text = {
        OutlinedTextField(hex, { hex = it; error = false }, label = { Text("RRGGBB hex color") }, isError = error, singleLine = true)
    }, confirmButton = { TextButton(onClick = {
        val input = hex.trim().removePrefix("#")
        val value = if (input.matches(Regex("[0-9a-fA-F]{6}"))) input.toLong(16).toInt() or 0xff000000.toInt() else null
        if (value != null) { onSelect(value); showCustom = false } else error = true
    }) { Text("Apply") } }, dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } })
}
