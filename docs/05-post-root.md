# 05. Post-root customization

Optional polish once the device is rooted, debloated and keystone-fixed: a rebuilt home screen, a consolidated settings app, boot logo and labels.

## 1. Custom home screen (launcher)

The home screen is a patch on top of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (`com.gathin.tvhome`, MIT). The patch and the modified files are in [`../launcher-mods`](../launcher-mods). It adds:

- **Weather** - keyless [Open-Meteo](https://open-meteo.com) (new `Weather.java`, needs the `INTERNET` permission; location in `strings.xml`, Budapest by default; refreshes every 30 minutes).
- **Second clock** - a home-country time zone (`vnClock`, `setTimeZone("Asia/Ho_Chi_Minh")`).
- **Lunar date** - Vietnamese lunisolar date and sexagenary year (built in; enabled via `lunar_date=true`, with the Can-Chi year on its own line).
- **Photo-frame screensaver** - `ClockScene.java` rewritten into a wallpaper slideshow (22-second cross-fade). Enabled via `clock_wallpaper=true` + `home_idle_clock_minutes=10`; shared by the system `ClockDream` and the home idle `ClockActivity`.
- **Three-column header** - Time / Date / Status, top-aligned, with the top-right buttons pinned to the top.

Apply it (see [`../launcher-mods/README.md`](../launcher-mods/README.md)): clone cleanyourtv, apply `hy300-home.patch` (or copy the files from `launcher-mods/src/`), then `JAVA_HOME=<jdk-22> sh ./gradlew assembleRelease` and `adb install -r app-release.apk`.

> Build note: on Windows, `cmd.exe /c gradlew.bat ...` does nothing here; use `sh ./gradlew assembleRelease`. The release build is debug-signed, so `adb install -r` overwrites cleanly.

### Screensaver behaviour on this firmware

`ClockDream` is already wired as the system screensaver (`settings secure screensaver_components=com.gathin.tvhome/.ClockDream`) and runs via `cmd dreams start-dreaming` (needs `su`). But the firmware's idle path just **powers the panel off**, and the Settings "Start now" button is broken (it sleeps instead of dreaming). Exiting a dream also blanks the panel; bring it back with `input keyevent KEYCODE_WAKEUP`. The launcher's own idle-clock (`home_idle_clock_minutes`) is the reliable path.

## 2. Hub app

[`../hub-app`](../hub-app) is `com.hy300.hub` ("Máy chiếu"): a single-screen launcher of tiles (Lucide-style icons) that consolidates the fragmented vendor settings - projector adjustment / keystone, casting, Wi-Fi, developer options, info. Non-exported targets are launched with a `su -c am start` fallback (grant the Hub root in Magisk once). Build with its own Gradle wrapper (`assembleDebug`).

## 3. Force the custom launcher as HOME

The firmware **hard-forces `com.htc.magcubicos` as HOME** at boot (it ignores preferred activity, the HOME role, `persist.sys.homePackage`, and `pm disable`). Bring the custom launcher forward from the Magisk module in [`../magisk-module/hy300tweaks`](../magisk-module/hy300tweaks): its `service.sh` waits for `sys.boot_completed` then `am start`s the launcher (a short ~3-second flash of magcubicos is accepted).

## 4. Labels, timezone, boot logo

- **Model / names / timezone:** edit `/oem/customer.prop` (a writable vfat; keep a `.bak`): change `HY300Plus` to your name and set the timezone. (`persist.sys.tpryauto` also lives here.)
- **Boot logo:** replace `bootlogo.bmp` (1280x720, 24-bit) inside the `bootloader_a` FAT, and/or drop a `bootanimation.zip` into `/oem/media/`. Back up the stock BMPs first. The custom logo used here is [`../assets/logo.png`](../assets/logo.png).

## 5. Recommended apps (32-bit)

This SoC is 32-bit (`armeabi-v7a`); 64-bit-only APKs and the Play-certified YouTube TV will not install. Use:

- **[Aurora Store](https://f-droid.org/packages/com.aurora.store/)** - anonymous Play downloads (no Google account).
- **[SmartTube](https://github.com/yuliskov/SmartTube)** - ad-free YouTube; pick an AVC (H.264) <= 1080p preset to dodge slow VP9/AV1 software decode.
- **[Material Files](https://github.com/zhanghai/MaterialFiles)** - light FOSS file manager (replaces the adware stock / ES explorer).
- **AirScreen** (via Aurora) - if the stock AirPlay (`com.mphotool.airplay`) stays broken.

## 6. zram

Already enabled by the firmware (low-RAM device).
