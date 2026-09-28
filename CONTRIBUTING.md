# Development

The project is a native Kotlin Android app. Keep the current-weather widget, optional device location and per-widget shortcut working when adding customization.

## Local setup

1. Install JDK 21 and Android SDK platform 37 / build-tools 36.0.0.
2. Set JAVA_HOME and ANDROID_HOME, or create an ignored local.properties with sdk.dir.
3. Use the checked-in Gradle wrapper. Do not commit machine paths, weather keys, keystores or build output.

Windows:

```powershell
$env:JAVA_HOME = 'C:\android-dev\jdk-21'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Linux/macOS:

```sh
bash ./gradlew assembleDebug testDebugUnitTest lintDebug
bash ./gradlew connectedDebugAndroidTest
```

The connected test command needs a running emulator/device. Default device tests use fixtures and controlled provider responses; no API key is required. The tests reset the installed debug app's settings and export example images to /sdcard/Download/RetroWeather.

Compose tests scroll the configuration container explicitly rather than relying on screen-size-dependent swipes. Test dependencies use Espresso 3.7.0 to avoid the removed reflective InputManager accessor on newer Android versions; see the [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test).

## Live smoke tests

These tests make real Open-Meteo requests, grant location permissions, inject a GPS fix, and pin a widget. They require the Pixel launcher used by the Pixel_9a development emulator. They are excluded from normal CI.

```powershell
.\gradlew.bat connectedDebugAndroidTest -PliveWeatherTests=true '-Pandroid.testInstrumentationRunnerArguments.class=com.retroweather.DeviceSmokeTests'
# Run denial coverage on its own fresh install, before a test grants location:
.\gradlew.bat connectedDebugAndroidTest -PliveWeatherTests=true '-Pandroid.testInstrumentationRunnerArguments.class=com.retroweather.DeviceSmokeTests#deniedLocationUsesSavedPositionWithoutCrashing'
```

Never use a personal daily-driver installation for destructive test fixtures. Uninstalling the app removes its local widget settings.

## Changes and review

- Check [acceptance criteria](docs/ACCEPTANCE.md) and [screenshot traceability](docs/SCREENSHOT_REVIEW.md).
- Keep artwork as explicit pixel masks in PixelArt.kt. Use the shared renderer for preview and widget.
- Give new persisted fields backwards-compatible defaults and bounded parsing.
- Cover behavior changes with relevant unit/device tests. Test transparency and layout with actual Android bitmaps.
- Export and inspect affected screenshots; update [verification notes](docs/VERIFICATION.md) with actual results and limitations.
- Keep product text functional. Do not add marketing slogans.
- Changes to weather requests must preserve coordinate precision, cache sharing, cooldowns, attribution and API-key privacy.

## CI and artifacts

The [Android workflow](.github/workflows/android.yml) builds the debug APK, runs JVM tests and lint, then runs deterministic device tests on API 26 and 35. It has read-only repository permissions and does not publish releases.

Open a successful workflow run in GitHub Actions and download its APK artifact. Reports and device screenshots are retained for 14 days. CI uses an ephemeral debug signing key, which may differ from your local key; replacing an APK signed with another key can require uninstalling it first.

A public release needs a maintainer-managed signing key and physical-device verification. Do not add a signing key to this repository. Source code and original artwork use the [MIT License](LICENSE); third-party software and weather data retain their own licenses and terms.
