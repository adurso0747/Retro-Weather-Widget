# Changelog

## 0.1.0 — Unreleased

Initial development version. Debug APKs can be built locally or downloaded from successful GitHub Actions runs after these changes are pushed. No signed release is published by the workflow.

- Native Kotlin setup app and resizable Android home-screen widget.
- Original pixel weather artwork in outline and solid styles, with day/night variants.
- Fixed location search/coordinates and optional GPS/network location.
- Open-Meteo current weather with optional WeatherAPI fallback, caching and scheduled refresh.
- Independent settings and launchable-app shortcut for each widget.
- Four layouts, sizing, alignment, padding, gradients, opacity, outlines and cutout effects.
- Tests for weather mapping, persistence, provider failure/recovery, rendering and setup controls.
- GitHub CI for builds, tests, lint and downloadable debug artifacts.

See [screenshot review](docs/SCREENSHOT_REVIEW.md) for remaining features and [verification](docs/VERIFICATION.md) for tested behavior and device limitations.
