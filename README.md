# Daily Snapshot

*~ My first attempt at Vibecoding with Claude*

A photo journaling app for Android that encourages you to capture one moment from your day — rendered as a Polaroid.

## What it does

Each day, you take a photo, write a short caption, and choose an optional filter. The app frames the result as a Polaroid print — warm paper-white border, handwritten-style typewriter caption, classic proportions — and stores it in your private gallery. A daily reminder nudges you to capture the moment before the day is gone.

Your gallery grows into a visual diary: photos presented as scattered Polaroids, tappable to view full-size with date and caption.

## Features

- **Camera** — capture with front/rear toggle; post-capture preview with Use / Retake
- **Polaroid framing** — automatic 1:1 center crop, classic border proportions, Special Elite typewriter font for captions
- **Filters** — Sepia, Faded, Noir, Warm, Cool — previewed live before saving
- **Gallery** — grid of tilted Polaroids, newest first
- **Detail view** — full-size framed image with date, caption, edit and delete actions
- **Daily reminder** — configurable time via WorkManager; toggleable from Settings
- **Material You** — dynamic color on Android 12+, warm retro palette on older devices

## Tech stack

Kotlin · Jetpack Compose · MVVM · Hilt · Room · WorkManager · CameraX · Coil 3

## Status

`1.0.0-alpha02` — core loop complete through Phase 3 (framing & theming). Gallery views, entry management, share/export, and widget in progress.

---
