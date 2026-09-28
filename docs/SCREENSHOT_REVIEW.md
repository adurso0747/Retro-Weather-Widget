# Screenshot requirements review

Reviewed all 31 supplied screenshots against the Kotlin app and the first-release AC. The user's five core requirements and per-widget app shortcut remain mandatory. Screenshot labels describe the reference app; they do not instruct this app to adopt its subscriptions, branding, claims about location storage, or weather-station source.

The original AC captured the core product but omitted most of the reference's detailed appearance controls. This implementation closes the current-weather appearance gaps below. Forecasts and other unfinished items remain explicit work, not completed acceptance criteria.

## Traceability

Screenshot numbers follow the order in the original request. Filename timestamps help locate the originals in D:\Software\Requirements.

| Screenshots / timestamp | Observed requirement | Current implementation |
| --- | --- | --- |
| 1 / 124043 | Live preview, refresh, last update, save, setup/style groups | Preview uses the actual widget renderer. Source and observation time shown; manual refresh. Save and Add are now always accessible at the bottom. Elapsed update wording remains a small UI gap. |
| 2 / 124056 | Fixed location, dynamic location, permission management, Open on Map | Fixed search/coordinates and optional GPS/background permissions already implemented. Added Open on map with a browser map fallback. |
| 3 / 124105 | Timezone and locale selection | Uses device locale/timezone for observation time. Explicit overrides and translated dates/content remain unfinished. |
| 4 / 124110 | Metric, imperial, custom units | Fahrenheit/Celsius implemented for current temperature. Other unit choices depend on additional weather fields and remain unfinished. |
| 5–9 / 124125–124136 | Icon above/below/left/right; forecast in middle | Four current-weather layouts implemented. Forecast layout remains unfinished. |
| 10–11 / 124149–124152 | Outline and solid themes; day/night variants; icon opacity | Added an independently authored solid pixel theme. Theme gallery and both day/night columns available. Added independent icon opacity. |
| 12 / 124204 | Background artwork and no background | Transparent background and solid/gradient base available. Original scenic background art remains unfinished. |
| 13–15 / 124212–124221 | Show text, dynamic text template, text size/padding/horizontal/vertical alignment | Temperature visibility already implemented. Added independent text size, padding and both alignments. Free-form templates and additional fields remain unfinished. |
| 16–18 / 124226–124231 | Show weather icon, icon size/padding/horizontal/vertical alignment | Visibility already implemented. Added independent size, padding and both alignments. Alignment takes effect on axes with space available in the chosen layout. |
| 19 / 124238 | Weather subtext visibility | Condition subtext is not yet implemented in the widget. Condition remains available through its accessibility description. |
| 20 / 124303 | Hourly/daily forecast, item count, forecast subtext | Not yet implemented. Requires forecast data, cache/schema extension, layout and timezone handling, and an explicit fallback plan when a provider lacks the requested forecast span. |
| 21–24 / 124354–124412 | Solid/gradient text color, multiple stops, reorder, presets, recents, opacity | Added two-to-four-stop gradients, stop editing/add/remove/reorder, direction, and opacity. Preset swatches/custom RGB input available. Reorder uses a button; drag gestures and recent-color history remain unfinished. |
| 25 / 124418 | Separate weather icon tint/gradient and opacity | Added independent icon gradients and opacity. Both themes are monochrome masks, so tint always applies; there is no intrinsic multicolor palette to restore. |
| 26 / 124426 | Base background enable/disable, solid/gradient, transparency | Added base gradients and independent opacity; transparent background toggle retained. |
| 27 / 124449 | Animation enable, speed, battery-aware disable | Not yet implemented. Needs launcher compatibility and battery testing; static frames remain the default. |
| 28–29 / 124456–124458 | Dynamic/manual sizing, global padding, horizontal/vertical alignment | Added fixed pixel size, outer padding, global alignment. Automatic fit retains integer scaling and reduces oversized requested element sizes for small hosts. |
| 30 / 124505 | Normal/cutout blend, contrast outlines | Added transparent cutout and configurable pixel outline. Without a visible base or outline, show an explanation and render normal content until the cutout can be visible. |
| 30–31 / 124505–124510 | One overlay across all content or background art | Per-element gradients implemented. A continuous global gradient across icon plus text, and overlays for scenic art, remain unfinished. |
| 29, 31 / 124458, 124510 | Launch a selected installed app on widget tap; change/clear mapping | Implemented per widget. Warm-task launch and missing-app recovery verified in the earlier smoke test. Shortcut remains in first-release AC. |

## Acceptance added in this pass

1. Theme selection affects preview, gallery and saved widget without changing weather or location.
2. Icon and text size/padding can be changed independently. Hidden elements reserve no layout space.
3. Global alignment and padding apply to all four layouts; element alignment uses spare space within its layout area.
4. Dynamic sizing fits available dimensions using whole pixel blocks. Fixed sizing is bounded by host space.
5. Icon, text and base have separate solid colors or two-to-four-stop gradients and opacity. Colors remain editable by preset or RGB value.
6. Gradient stops can be selected, added, removed and reordered without affecting another element's palette.
7. Contrast outlines remain crisp. Cutout pixels reveal the wallpaper through a visible base or outline.
8. Old saved configurations load with outline theme, centered automatic sizing and full opacity. Location and shortcut mapping are preserved.
9. Every detailed appearance editor includes a live preview. Closing it keeps the draft; Save widget persists it.
10. Fixed or resolved device location can open in a map app, with a browser fallback.

## Remaining implementation order

1. Current-weather subtext and custom text fields, with timezone/locale controls.
2. Hourly/daily forecast data and layout, including truthful provider/fallback availability.
3. Recent colors and continuous global overlays.
4. Original scenic pixel backgrounds.
5. Optional animation only after launcher and battery validation.

These are reference-derived candidates, separate from the mandatory current-weather and shortcut acceptance criteria. No Pro tier, payments, ads, borrowed branding, or copied artwork are planned.
