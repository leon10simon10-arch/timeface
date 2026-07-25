# TimeFace — Wear OS watch face

Analog + digital watch face that shows your next calendar events, with a
minute-tick dial and a blue arc showing exactly how long your next event runs.

## What's in the box

- `app/` — the Wear OS app (Kotlin, androidx.wear.watchface)
  - `service/TimeFaceWatchFaceService.kt` + `TimeFaceRenderer.kt` — draws the
    dial, hands, digital clock, event arc, and the event cards at the bottom
  - `service/NextEventComplicationService.kt` — a bonus complication data
    source (next event as ranged-value/text) if you ever want to reuse it in
    another watch face
  - `ui/MainActivity.kt` — the one-time permission screen (calendar access)
  - `CalendarRepository.kt` — reads events from the system Calendar Provider
- `.github/workflows/build.yml` — builds a debug APK on every push and
  uploads it as a downloadable Actions artifact (no local Android Studio
  needed)

## Get a build

1. Create a new GitHub repo and push this folder to it:
   ```bash
   cd wearwatchface
   git init
   git add .
   git commit -m "Initial TimeFace watch face"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. Go to the repo's **Actions** tab — the "Build APK" workflow runs
   automatically. When it's green, open the run and download the
   `TimeFace-debug-apk` artifact (a zip containing `app-debug.apk`).

## Install it on the watch

Debug APKs aren't on the Play Store, so sideload it:

- **ADB over Wi-Fi** (easiest, no phone needed): on the watch, enable
  Developer Options → Wireless debugging, then from a computer:
  ```bash
  adb connect <watch-ip>:<port>
  adb install app-debug.apk
  ```
- Or use a sideload tool like "Wear Installer 2" from the Play Store on your
  phone, pointed at the APK.

After installing:
1. Open the **TimeFace** app icon on the watch once and tap
   "Grant calendar access" — required so the face can read your events.
2. Long-press the watch face screen → pick **TimeFace** from the list.

## Notes / next steps

- The arc/chip currently shows only the *next* event; the card below it
  lists up to two upcoming events, matching the mock-up.
- Ambient (always-on) mode currently draws a simplified static face without
  the event card, to save battery — easy to expand later.
- No signing config is set up, so this is a debug build only (fine for
  sideloading onto your own watch, not for the Play Store).
