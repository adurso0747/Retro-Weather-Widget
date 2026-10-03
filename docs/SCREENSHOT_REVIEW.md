# Screenshot requirements audit

Audit date: October 2, 2026. **Full screenshot feature parity is incomplete.** The core current-weather widget and per-widget app shortcut are implemented, but several reference features have no corresponding UI, stored settings, data or renderer support.

This audit compares all 31 screenshots supplied in the original request with source and test assertions. The initial audit was a source review; rows 13, 16 and 19 were updated during the October 2 implementation follow-up for editable text and visible descriptions. Execution results are in [verification notes](VERIFICATION.md). The [acceptance criteria](ACCEPTANCE.md) distinguish the original release scope from outstanding screenshot requirements.

## Reading the results

- **Implemented:** the visible functional controls have corresponding code. This does not mean every combination has passed a device test.
- **Partial:** some visible features exist, but others are absent or behave differently.
- **Missing:** the main feature has no implementation.

The screenshots are requirements evidence, not instructions to copy reference branding, subscriptions, promotional copy, artwork or privacy claims. Original pixel artwork and functional equivalents are appropriate; identical carousels, tabs and card styling are not required for functional parity. Controls whose behavior is not demonstrated are identified separately rather than guessed.

Original files are in `D:\Software\Requirements`, named `Screenshot_20260920_<timestamp>_Retro Mode Weather.jpg`. Every timestamp is listed below. These files are external references, so this document does not create broken repository image links to them.

## All 31 screenshots

Source keys link to the implementing files in the next section. Repeated collapsed section labels are covered in their expanded screenshots.

| # | Timestamp | Visible features | Status | Evidence and remaining work |
| --- | --- | --- | --- | --- |
| 1 | 124043 | Configuration, live preview, palette/pushpin controls, last-update age, refresh, Save | Partial | UI shares the widget renderer and has refresh plus persistent Save/Add buttons. It shows absolute observation time, not elapsed time since the last successful update. The main preview scrolls away; reference palette/pushpin controls have no equivalents. Their exact behavior is not demonstrated. |
| 2 | 124056 | Fixed location search, Open on Map, dynamic location, permission management | Implemented | UI + Location + Store support fixed city/coordinates, GPS/network location, permission settings and map/browser launch. Latest coordinates are stored locally for recovery; the reference's immediate-disposal claim is not our implementation. |
| 3 | 124105 | Timezone selection with offset/time, locale selection for weekday/month names, font fallback | Missing | UI formats observation time using device defaults. Settings have no locale/timezone override. Weather labels are English. No localized date placeholders or non-Latin widget font fallback exist. |
| 4 | 124110 | Metric, Imperial and Custom presets; time, temperature, wind, precipitation, pressure and distance units | Partial | Settings support Fahrenheit/Celsius only. There are no full presets, custom per-measurement units or 12/24-hour preference. Weather data contains none of the other measurements. |
| 5 | 124125 | Icon Top layout | Implemented | Settings and Renderer place the icon above temperature. |
| 6 | 124128 | Icon Bottom layout | Implemented | Settings and Renderer place the icon below temperature. |
| 7 | 124131 | Icon Left layout | Implemented | Settings and Renderer place the icon left of temperature. |
| 8 | 124133 | Icon Right layout | Implemented | Settings and Renderer place the icon right of temperature. |
| 9 | 124136 | Forecast Middle layout | Missing | Settings have only four current-weather layouts. No forecast strip/layout exists. |
| 10 | 124149 | Classic outline theme, day/night and playback preview controls, icon opacity | Partial | Art + Editor provide original outline masks and opacity. The gallery shows day/night columns. It has no interactive day/night preview toggle or play/pause behavior. |
| 11 | 124152 | Solid theme, day/night and playback preview controls, icon opacity | Partial | Art + Editor provide original solid masks and opacity. The same preview/playback gaps as screenshot 10 remain. |
| 12 | 124204 | Background theme selection and No theme | Partial | Editor supports no background or a solid/gradient base. No scenic pixel-art theme selector or scene artwork exists. The partially visible reference scene does not establish a full catalog. |
| 13 | 124212 | Show Text, editable dynamic text such as `{temperature:unit}`, size | Implemented | Text editor supports seven current-weather/location/observation-date fields, validation, preview, reset/cancel and per-widget persistence. Explicit Latin pixel glyphs wrap with overflow dots. Main text visibility/size remain independent. Locale overrides and non-Latin glyphs are still absent. |
| 14 | 124217 | Text size, padding and horizontal alignment | Implemented | Editor + Appearance + Renderer expose independent size, padding and alignment. Size uses discrete choices; alignment takes effect where the layout leaves spare space. |
| 15 | 124221 | Text horizontal and vertical alignment | Implemented | Both text alignment axes are stored and rendered where space permits. |
| 16 | 124226 | Weather element visibility, size, padding; Subtext tab | Implemented | Icon visibility, size and padding exist. Weather description is available as a separate toggle rather than a tab; see screenshot 19. |
| 17 | 124229 | Icon size, padding, horizontal and vertical alignment | Implemented | Editor + Appearance + Renderer support these independently of text, subject to available layout space. |
| 18 | 124231 | Icon horizontal and vertical alignment | Implemented | Both icon alignment axes exist. The collapsed Animation section is assessed in screenshot 27. |
| 19 | 124238 | Weather subtext visibility beside the icon | Implemented | Optional condition text appears beneath the icon, independently of main text. It shares the icon treatment and hides with the icon. Small widgets can omit it to retain readable main content. |
| 20 | 124303 | Forecast visibility, Hourly/Daily, item count, Element/Subtext tabs | Missing | API + Settings + Renderer support current weather only. No forecast entries, count, rendering or subtext exist. Five days are selected in the screenshot; the full count range and unopened Subtext options are not shown. |
| 21 | 124354 | Text solid/gradient color, editing, expandable presets/recents | Partial | Editor supports solid colors, editable gradients and solid swatches. No gradient preset library, recent history or expandable preset/history panel exists. |
| 22 | 124358 | Gradient presets, recent colors, text opacity | Partial | Text opacity exists. Preset gradients and recent history are absent. |
| 23 | 124406 | Gradient stop editing/addition, drag reorder, gesture hint | Partial | Editor supports two to four stops, editing, addition/removal and Move earlier. It has no drag reorder or hideable gesture hint. The reference maximum stop count is not shown. |
| 24 | 124412 | Presets/recents panel and text opacity | Partial | Opacity exists; gradient presets, recents and their hide/show control do not. |
| 25 | 124418 | Icon overlay enable, solid/gradient tint, presets/recents | Partial | Independent tint/gradient is always applied to monochrome masks. No overlay enable/disable setting, text-color inheritance when disabled, gradient preset library or recent history exists. |
| 26 | 124426 | Background enable, transparent/solid/gradient base, presets/recents | Partial | Editor has transparency, solid/gradient colors and opacity. Gradient presets and recents are missing. Disabling the base clears its color; re-enabling uses a default rather than restoring the previous color. |
| 27 | 124449 | Icon/background animation, battery-aware disabling, Normal/Slow/Slowest | Missing | Art has static sprites. No frames, playback settings, animation scheduler, battery threshold or power-saver behavior exists. |
| 28 | 124456 | Dynamic sizing, outer padding, overall horizontal/vertical alignment | Implemented | Editor + Renderer support automatic fit, fixed pixel size, padding and alignment. Very small host bounds need additional clipping regression coverage; see below. |
| 29 | 124458 | Overall alignment and selected app shortcut summary | Implemented | Alignment and per-widget shortcut label are present. Full shortcut controls are assessed in screenshot 31. |
| 30 | 124505 | Normal/Cutout, contrast outlines, global Content/Theme overlays | Partial | Editor + Renderer implement cutout, outlines and an explanation for invisible cutout configurations. No continuous overlay spanning all content or overlay for scene artwork exists. Independent gradients are not equivalent. |
| 31 | 124510 | Global overlay toggle; selected shortcut app, change/remove mapping | Partial | UI + Widget + Store implement app selection, replacement, clearing, per-widget persistence and launch/missing-app fallback. Global overlays are absent. The shortcut row shows the label rather than the reference app icon/activity details. |

## Source evidence

| Key | Source | What was checked |
| --- | --- | --- |
| UI | [MainActivity.kt](../app/src/main/java/com/retroweather/MainActivity.kt) | Setup, preview, timestamp, map launch, gallery, app picker, shortcut launch and clearing. |
| Editor | [AppearanceEditor.kt](../app/src/main/java/com/retroweather/AppearanceEditor.kt) | Size/alignment, gradients, opacity, transparency, dimensions and effects. |
| Settings | [Models.kt](../app/src/main/java/com/retroweather/data/Models.kt) | Four layouts, current-only weather, location, temperature and shortcut fields. |
| Appearance | [Appearance.kt](../app/src/main/java/com/retroweather/data/Appearance.kt) | Persisted fields, bounds and migration defaults. |
| Art | [PixelArt.kt](../app/src/main/java/com/retroweather/art/PixelArt.kt) | Explicit outline/solid masks, day/night variants and temperature glyphs. |
| Renderer | [PixelRenderer.kt](../app/src/main/java/com/retroweather/art/PixelRenderer.kt) | Icon, optional condition description and bounded template text; layouts, independent colors, scaling, outlines and cutout. PixelText provides explicit Latin glyphs and wrapping. |
| API | [WeatherApi.kt](../app/src/main/java/com/retroweather/data/WeatherApi.kt) | Only current temperature, condition and day/night are requested/parsed. Calling Open-Meteo's endpoint named `forecast` with current fields does not implement forecasts. |
| Location | [DeviceLocation.kt](../app/src/main/java/com/retroweather/data/DeviceLocation.kt) | Permissions, approximate/precise fixes, age checks and timeout. |
| Store | [AppStore.kt](../app/src/main/java/com/retroweather/data/AppStore.kt) | Per-widget JSON, last position, weather cache and encrypted fallback key. |
| Widget | [WeatherWidget.kt](../app/src/main/java/com/retroweather/widget/WeatherWidget.kt) | Presentation, accessibility, tap dispatch, resize and updates. |

## Test evidence and limits

- [WeatherTests](../app/src/test/java/com/retroweather/WeatherTests.kt): provider mappings, temperature conversion, staleness, settings, coordinates, sprite/glyph structure and current-weather parsing.
- [AppearanceTests](../app/src/test/java/com/retroweather/AppearanceTests.kt): migration, appearance round trips, corrupt-value bounds and solid artwork.
- [AppearanceDeviceTests](../app/src/androidTest/java/com/retroweather/AppearanceDeviceTests.kt): selected opacity/cutout, gradient/outline and size/alignment assertions; one real theme-edit/save flow. Exporting image sheets aids visual review but does not prove every feature.
- [IntegrationTests](../app/src/androidTest/java/com/retroweather/IntegrationTests.kt): controlled outage/recovery/rate limits, cache isolation, encrypted storage, rendering and opening the shortcut picker.
- [DeviceSmokeTests](../app/src/androidTest/java/com/retroweather/DeviceSmokeTests.kt): opt-in live network/location, pin, selected-app launch and missing-app recovery. These depend on device state and are not the default CI suite.

The September 27 baseline was 16 JVM tests and 12 default API-37 device tests. The October 2 follow-up adds six template/font/wrapping unit tests and two device tests for text rendering and the real editor/save flow. See verification notes for run results. No existing test establishes parity for missing features.

Additional validation is needed for all independent element-alignment combinations, extreme negative-temperature/outline combinations, background disable/enable, shortcut clearing and selected-app updates, map fallback, and actual phone battery/launcher behavior. The text follow-up tests four layouts at three sizes, including 32×32, and bounds main text with wrapping/ellipsis. This is not exhaustive proof for every appearance combination.

## Work required for full parity

These are open requirements, not completed acceptance criteria or merely optional candidates. Detailed checkboxes are in [the parity acceptance backlog](ACCEPTANCE.md#screenshot-parity-backlog).

1. Add timezone/locale, non-Latin font support and full unit preferences (3, 4). Current-weather templates and visible subtext are now implemented (13, 19).
2. Add hourly/daily data, persistence, count/subtext and Forecast Middle layout (9, 20). Define fallback behavior for unsupported fields/spans.
3. Complete color presets/history, drag reorder, icon overlay enable/inheritance and global overlays; preserve base color when toggled (21–26, 30–31).
4. Author original scenic pixel backgrounds and add theme selection (12).
5. Implement and validate animation, speed and battery behavior, including previews (10–11, 27). Verify launcher feasibility before calling this done.
6. Complete elapsed update presentation and specify the reference preview palette/pushpin behavior (1). Add shortcut visual details if exact presentation parity is desired (31).
7. Add targeted tests for new behavior and complete device checks before declaring parity.

The navigation menu, feedback bubble and Pro buttons do not reveal additional functional requirements. No payment system, copied identity or unshown menu behavior is inferred.
