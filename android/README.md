# Pemeriksaan IFP

Android checklist for classroom interactive flat panels. It follows `Launcher/launcher.py`: one check at a time, skip anything the panel does not have (`Dilewati`), then write a report. The Python launcher is unchanged.

- minSdk 24 (Android 7.0)
- targetSdk 36
- Package `id.rcikaym.ifplauncher`

The UI is Indonesian. English strings in `res/values` are the fallback.

## Build

JDK 17+ and Android SDK platform 36.

```bash
cd android
./gradlew assembleDebug
./gradlew test
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Sideload

USB drive:

1. Copy `app-debug.apk` onto a USB drive.
2. Plug it into the panel and open the APK from the file manager.
3. If Android blocks it, allow that file manager under Settings → Security → Install unknown apps.
4. Open **Pemeriksaan IFP**.

adb, from this folder:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On a panel that only exposes network debugging: `adb connect <ip>:5555`, then the same install.

## Find the Android version

- Settings → About (or About tablet) → Android version.
- The first screen of this app shows the version and SDK.
- From a computer: `adb shell getprop ro.build.version.release` and `adb shell getprop ro.build.version.sdk`.

JSON reports use stable `pass` / `fail` / `skip` tokens. Titles follow the UI language.
