# First-release acceptance criteria

The five user requirements and configurable app shortcut are mandatory. See [screenshot review](SCREENSHOT_REVIEW.md) for all reference-derived features, implementation status, and remaining work.

These are acceptance requirements, not a list of passed checks. The October 2 audit found that the original release scope does **not** cover all screenshot features. Full screenshot parity remains incomplete; the backlog below records the additional acceptance work.

## Artwork and presentation

- Original weather art is built from explicit pixels on a 32×32 grid, not generated pictures.
- Outline artwork follows the supplied references: round sun and crescent, overlapping clouds, diagonal rain, stepped lightning, and horizontal fog bars.
- Setup uses functional labels without promotional slogans.
- All documented Open-Meteo WMO codes and WeatherAPI current condition codes map to a weather family; unknown codes have a neutral fallback.
- Clear and partly cloudy states have day/night variants; drizzle, rain, freezing precipitation, snow, showers, thunderstorms, hail, fog, and overcast are represented.
- Preview and widget use the same pixel renderer. Resizing preserves whole pixel blocks.
- °F/°C, four icon positions, text/icon visibility, scale, independent solid colors, custom RGB colors, and transparent/solid background are configurable.
- At least one of icon and main widget text remains visible.
- Main text supports validated, per-widget templates for temperature (with or without units), unit, condition, location, observation time and observation date. Old configurations retain temperature-only text.
- An optional condition description appears beneath the weather icon, including when main text is hidden. It follows icon color/opacity and is hidden with the icon.
- Text uses explicit pixel glyphs, wraps within available bounds and marks overflow with dots. The editor explains current Latin-font and device-timezone limitations.
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

## Screenshot parity backlog

Status updated October 2, 2026 after the first implementation follow-up. Numbers reference the individually indexed screenshots in [the audit](SCREENSHOT_REVIEW.md). Checked items describe the supported scope; locale, additional provider fields and forecasts remain separate open work.

- [ ] **Locale/timezone (3):** select and persist a timezone and locale per widget; display the timezone's current offset/time; use it for forecast, sunrise/sunset and date fields. Localize weekday/month names and provide readable fallback glyphs for unsupported scripts. Test daylight-saving transitions and device timezone changes.
- [ ] **Units (4):** provide Metric, Imperial and Custom preferences for time format, temperature, wind speed, precipitation, pressure and distance. Obtain the corresponding weather fields, label units correctly and test conversion/rounding. Do not display fabricated values when a provider lacks a field.
- [x] **Dynamic text (13):** edit, preview and persist validated templates with seven current-weather/location/observation-date fields, including `{temperature:unit}`. Field buttons, missing-data markers, cancel/reset, explicit pixel capitals, wrapping and overflow dots are implemented. Tests cover validation, old configurations, persistence and rendering. Non-Latin glyphs and locale overrides remain under locale/timezone above.
- [x] **Weather subtext (19):** show/hide the condition beneath the weather icon; include it in preview, sizing, accessibility and saved configuration. It follows icon color/opacity and hides with the icon. Small hosts can omit it to preserve readable main content.
- [ ] **Forecast (9, 20):** add hourly/daily data and cache support, show/hide, item count, forecast subtext controls and Forecast Middle layout. Test ordering across local midnight/DST, missing entries, offline data and fallback-provider differences. The screenshot shows five days selected, but the supported range and unopened subtext options still need specification.
- [ ] **Color library/history (21, 22, 24–26):** add gradient presets and persisted recent colors/gradients, with the displayed hide/show behavior. Keep text, weather and base editing independent.
- [ ] **Gradient gestures (23):** support dragging stops to reorder and a dismissible gesture hint; retain an accessible button alternative. Test order persistence and gradient output.
- [ ] **Icon overlay (25):** enable/disable the custom icon overlay, with explicit default/text-color inheritance when disabled. Preserve the custom palette for re-enabling and test its interaction with opacity and global overlays.
- [ ] **Background toggle (26):** preserve and restore the chosen background color/treatment when disabled/re-enabled; test transparency and zero opacity.
- [ ] **Global overlays (30, 31):** independently enable an overlay across combined content and an overlay over background artwork. Use common bounds so gradients continue across elements. Define precedence over individual colors, opacity, outlines and cutout; test the combined result.
- [ ] **Background themes (12):** provide original, explicitly placed pixel scene artwork and a No theme choice; keep scene selection distinct from base color. Include preview and saved-widget rendering. The screenshots do not specify a complete scene catalog.
- [ ] **Animation (10, 11, 27):** author explicit frames and support playback enable, Normal/Slow/Slowest speed, power-saver disabling and a documented low-battery threshold consistent with the shown 20–25% range. Add day/night and play/pause theme previews. Verify supported launchers, screen-off behavior, process recovery and battery use before sign-off.
- [ ] **Update age (1):** show elapsed time since a successful weather refresh without confusing it with observation time; maintain correct stale/offline status and manual refresh behavior.
- [ ] **Preview controls (1):** specify and implement the intended palette/pushpin behavior. The screenshot shows the controls but does not demonstrate their actions; pinning the home-screen widget is not proof of preview pinning.
- [ ] **Sizing and device validation (14–18, 28–29):** test all independent element alignment combinations and extreme supported widget bounds/temperature lengths. Define a readable minimum-size/overflow policy and verify it on the launcher.
- [ ] **Shortcut completion checks (29, 31):** verify clear/change and app-update persistence on a device in addition to existing launch/missing-app checks. If matching the reference shortcut row's presentation, include the selected app icon and activity detail. App-specific deep links are not shown and remain outside this requirement.

Full parity requires these checks plus the earlier core acceptance criteria. A passing current-weather test suite alone does not close this backlog.
