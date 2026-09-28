# Verification

## September 27, 2026 — appearance and build automation

The appearance work adds solid pixel masks, separate icon/text/background gradients and opacity, element sizing/padding/alignment, fixed or automatic sizing, contrast outlines and cutout rendering. Detailed editors share the widget renderer and save through the existing per-widget settings. The main Save and Add buttons remain visible while scrolling.

New coverage includes legacy configuration migration, full appearance persistence, corrupt-value bounds, both art themes, alpha/cutout pixels, gradient endpoints, outlines, independent element dimensions, and editing/saving through the actual setup screen. UI tests use Compose scrolling to reach controls without relying on device-specific swipe distances.

| Local check | Result |
| --- | --- |
| Debug APK build | Passed |
| JVM tests | 16 passed; no failures or skips |
| Default device tests (API 37) | 12 passed; no failures or skips |
| Android lint | 0 errors, 28 warnings |
| Git whitespace/diff check | Passed |

Verified together with `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest`. Existing JVM results were reused by Gradle when inputs were unchanged. The device suite ran all 12 tests on the emulator. The appearance controls now wrap on small screens, and the editor test verifies selecting Solid, saving, and preserving another widget's separate theme. Lint warnings concern dependency updates, KTX suggestions and compatibility attributes/rules; they are not suppressed by a baseline.

A subsequent focused `exportAppearanceExamples` device run passed and exported the actual launcher drawable for the README. That run overwrites the connected-test report with its single result. `buildEnvironment` confirmed the resolved Kotlin Gradle plugin is 2.2.10. The full-suite counts above refer to the preceding complete run.

The default connected suite uses controlled weather responses. Live weather/GPS/launcher tests are opt-in. The live suite orders denial coverage before granting location, and still requires a fresh install for that case.

The GitHub Actions workflow builds the APK, runs unit tests and lint, and configures emulator checks on API 26 and 35. It uploads the debug APK, test reports and screenshots for 14 days. The workflow has been reviewed locally but has not been run on GitHub; those two emulator versions remain unverified until CI executes after push. Local device checks use API 37.

Visual examples: [appearance combinations](images/RetroWeather/appearance-examples.png), [outline and solid icon sheet](images/RetroWeather/theme-icons.png), [icon editor](images/RetroWeather/icon-settings.png). These are exported from the Android renderer using fixture weather; they are not live weather observations. Live-provider and launcher checks below were performed on September 25 and were not rerun for this appearance/test tooling pass.

## September 25, 2026 — core widget

Built with the Android SDK and JDK under `C:\android-dev`. Device checks used the Pixel_9a emulator (Android 17 / API 37). The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Results

| Check | Result |
| --- | --- |
| Debug APK build | Passed |
| JVM unit tests | 12 passed |
| Integration tests | 7 passed after correcting the UI test's scrolling gesture |
| Live Open-Meteo current weather and city search | Passed |
| Granted device location | Passed with an injected GPS fix |
| Denied location | Passed separately on a fresh install; retains saved position and reports missing permission |
| Launcher pin confirmation and per-widget shortcut persistence | Passed |
| Widget tap launches Settings while setup is already in recents | Passed after correcting activity launch flags |
| Missing selected app | Opens configuration with an explanation; passed |
| Widget resize render | Passed through widget options update |
| Android lint | No errors; 28 warnings (dependency updates, KTX suggestions, and compatibility attributes/rules) |
| Whitespace/diff check | Passed |

The seven integration cases cover encrypted key storage, independent widget settings, primary outage/fallback/recovery, both-provider failure and location-specific cache isolation, missing fallback key, HTTP rate-limit cooldown, exact renderer colors, and the setup/shortcut selection screen. WeatherAPI fallback is tested with controlled responses, not a live account key.

The permission-denial test explicitly skips if location is already granted, rather than silently passing without checking anything. Run on a fresh installation for actual denial coverage.

## Visual review

- [Weather icon sheet](images/RetroWeather/sprite-sheet.png): final original 32×32 masks, day/night columns. Reviewed for rounded sun outlines, overlapping cloud silhouettes, diagonal rainfall, visible thunderbolts, snow distinctions, and horizontal fog bars.
- [Configuration screen](images/RetroWeather/configuration.png): final artwork and functional labels. The promotional header, subtitle, section titles, gallery title, and footer were removed or replaced.
- [Home widget](images/RetroWeather/home-widget.png): launcher placement captured during the shortcut smoke test, before the final cloud-overlap adjustment.

All weather artwork is drawn from explicit pixel cells. Both the widget and app preview use the same renderer, with integer scaling and no smoothing.

## Reproduce

```powershell
$env:JAVA_HOME = 'C:\android-dev\jdk-21'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -PliveWeatherTests=true '-Pandroid.testInstrumentationRunnerArguments.class=com.retroweather.DeviceSmokeTests'
.\gradlew.bat connectedDebugAndroidTest -PliveWeatherTests=true '-Pandroid.testInstrumentationRunnerArguments.class=com.retroweather.DeviceSmokeTests#deniedLocationUsesSavedPositionWithoutCrashing'
```

The live smoke tests require internet access and the Pixel emulator launcher. They add a home-screen widget and temporarily grant location/mock-location access. Screenshots are exported to `/sdcard/Download/RetroWeather/`. Gradle's connected-test report is overwritten by each run, so the results above describe the separate runs.

## Remaining device checks

Before treating this as a signed release, verify on the intended phone/launcher: resizing gestures, widget-picker setup/cancel, shortcut clearing and app updates, approximate/background location while moving, reboot/process recovery, and overnight refresh under battery saver/Doze. Emulator checks do not establish battery consumption or OEM background reliability. Thirty-minute scheduling remains a target, not an exact delivery guarantee. Live WeatherAPI fallback requires the user's own free key.
