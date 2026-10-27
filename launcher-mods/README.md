# launcher-mods - custom home screen

These are **modifications on top of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)** (`com.gathin.tvhome`, MIT) - **not** a full launcher. See [`../docs/05-post-root.md`](../docs/05-post-root.md) for what they do (weather, second clock, Vietnamese lunar date, photo-frame screensaver, 3-column header).

## What's here

- `hy300-home.patch` - a `git diff` of the 7 modified files **plus** the new `Weather.java`.
- `src/` - plain copies of all 8 changed/added files, at their real paths under the launcher, in case the patch doesn't apply cleanly against a newer upstream.

| File | Change |
|---|---|
| `app/src/main/java/com/gathin/tvhome/Weather.java` | **new** - Open-Meteo weather (keyless) |
| `app/src/main/java/com/gathin/tvhome/MainActivity.java` | wire weather, second clock, lunar |
| `app/src/main/java/com/gathin/tvhome/ClockScene.java` | screensaver → wallpaper slideshow |
| `app/src/main/AndroidManifest.xml` | add `INTERNET` permission |
| `app/src/main/res/layout/activity_main.xml` | 3-column header, top-aligned |
| `app/src/main/res/values/config.xml` | enable lunar / weather / second clock / screensaver |
| `app/src/main/res/values/strings.xml` | weather location, second-clock format, short date |
| `app/src/main/res/values-vi/lunar.xml` | Can-Chi year on its own line |

## Apply

```sh
git clone https://github.com/Gathin23/cleanyourtv
cd cleanyourtv
git apply /path/to/hy300-home.patch        # or copy launcher-mods/src/** over launcher/**
cd launcher
JAVA_HOME=/path/to/jdk-22 sh ./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Build gotcha (Windows):** `cmd.exe /c gradlew.bat …` produces no output here - use `sh ./gradlew`. The release APK is debug-signed, so `adb install -r` overwrites an existing install.

**Configure the weather location:** edit `weather_latitude` / `weather_longitude` / `weather_label` in `res/values/strings.xml` (default is Budapest). The second-clock time zone is `second_clock_tz` (default `Asia/Ho_Chi_Minh`).
