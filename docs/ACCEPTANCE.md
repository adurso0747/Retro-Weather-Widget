# First-release acceptance criteria

The five user requirements and configurable app shortcut are mandatory. See [screenshot review](SCREENSHOT_REVIEW.md) for all reference-derived features, implementation status, and remaining work.

## Artwork and presentation

- Original weather art is built from explicit pixels on a 32×32 grid, not generated pictures.
- Outline artwork follows the supplied references: round sun and crescent, overlapping clouds, diagonal rain, stepped lightning, and horizontal fog bars.
- Setup uses functional labels without promotional slogans.
- All documented Open-Meteo WMO codes and WeatherAPI current condition codes map to a weather family; unknown codes have a neutral fallback.
- Clear and partly cloudy states have day/night variants; drizzle, rain, freezing precipitation, snow, showers, thunderstorms, hail, fog, and overcast are represented.
- Preview and widget use the same pixel renderer. Resizing preserves whole pixel blocks.
- °F/°C, four icon positions, text/icon visibility, scale, independent solid colors, custom RGB colors, and transparent/solid background are configurable.
- At least one of icon and temperature remains visible.
- Outline and solid pixel themes are selectable; previews and day/night gallery reflect the selected theme.
- Icon and text have independent size, padding, horizontal and vertical alignment controls.
- Icon, text and background each support opacity and solid colors or two-to-four-stop gradients. Stops can be edited, added, removed and reordered.
- Automatic/fixed sizing, outer padding and overall alignment preserve whole pixel blocks. Fixed size is bounded by the host's available space.
- Contrast outlines and transparent cutout mode are available. Invisible cutout configurations fall back to normal content with an explanation.
- Existing saved widgets retain their settings when the new appearance fields are absent.
- Detailed appearance controls show a live preview; Save remains accessible on the main screen.

## Setup and location

- Fixed city/postal-code search disambiguates region/country. Direct latitude/longitude input validates ranges.
- Fixed mode works without location permission.
- A chosen fixed location or resolved device location can open in a map app, falling back to a browser map.
- Dynamic mode accepts approximate or precise Android location; stale fixes are checked and acquisition times out.
- Background permission has a separate explanation and settings flow.
- Denial, disabled GPS, or missing background permission preserves usable cached data and explains limitations.

## Weather and reliability

- Open-Meteo supplies current weather without a paid subscription or API key for personal non-commercial use.
- WeatherAPI is an optional fallback with the user's free key, encrypted on-device.
- Refresh targets 30 minutes while widgets exist; exact timing is not promised.
- Duplicate locations reuse weather; manual refresh is throttled.
- Provider outages, malformed/stale responses, and rate limits trigger bounded fallback/cooldown behavior.
- Recovery returns to primary. Both-provider failure preserves only weather for the matching location.
- Data source and weather time are visible in the app. Stale widget data has an amber marker and accessibility description.

## Widget lifecycle

- Add from the app using launcher pin confirmation, or configure through the widget picker.
- Save applies to the selected widget; canceling initial configuration leaves no saved instance settings.
- Multiple widgets can have independent configurations.
- Resize redraws the widget. WorkManager persists normal scheduled work across process death/reboot.
- Deleting widgets cleans up configuration; removing the last widget stops periodic work.
- Android force-stop and OEM background restrictions can require reopening the app.

## App shortcut (required in release one)

- Pick an installed launchable app per widget.
- Tap opens the selected app.
- Change or clear the mapping; clearing opens Retro Weather.
- Mapping survives process restart and updates.
- Missing/unlaunchable selected app opens Retro Weather with an explanatory message.
- All widget settings remain accessible from the Retro Weather launcher icon.
- App-specific deep links are outside release one.

## Delivery

- Kotlin source and reproducible Gradle wrapper in the repository.
- Buildable debug APK; unit and instrumented tests; Android lint without errors.
- README includes setup, permissions, provider terms, privacy, build steps, and limitations.
- GitHub Actions builds the APK and runs unit tests, lint and deterministic emulator tests; debug APK and reports are available as workflow artifacts after a successful run.
- Live-provider and launcher smoke tests are documented separately and do not require secrets in routine CI.
- Physical-device battery/launcher validation remains a release sign-off task, not something an emulator can establish.
