<div align="center">

# HY300 Ultra - Allwinner H726 - All-in-One

Root, reverse-engineering, malware removal and customization of the
**Gojodoq / Magcubic HY300 Ultra** projector - Allwinner **H726 (sun50iw15 / FEL 0x1911)**.

**English** · [Tiếng Việt](README.vi.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![SoC](https://img.shields.io/badge/SoC-Allwinner%20H726%20(sun50iw15)-blue)
![FEL ID](https://img.shields.io/badge/FEL%20ID-0x1911-informational)
![Android](https://img.shields.io/badge/Android-14-green)
![Root](https://img.shields.io/badge/root-Magisk-success)
![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)
![Affiliation](https://img.shields.io/badge/affiliation-none-lightgrey)

<img src="assets/screenshots/home-3col.png" width="680" alt="Custom home screen">

</div>

---

## What this is

The HY300 Ultra is a cheap cylindrical **Android 14** projector sold under many brands - Gojodoq, Magcubic and others - all built on **one white-label Allwinner H726 board**. Out of the box it is locked down: ad/tracker bloat, a **BadBox-family backdoor baked into the firmware**, flaky auto-keystone, a 32-bit userspace that rejects many apps, and no official way to root or modify it.

This repository is the **complete, reproducible record of opening one such unit up and making it yours** - gaining root, reverse-engineering the display/keystone subsystem, removing the malware, and rebuilding the software to taste - with **every external source cited**.

It is written to **complement, not replace, the existing [XDA root guide](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/)**. That guide shows *how to flash*; this repo documents what it does not: the checksum trick that actually makes a custom image flash, the internals of the keystone system, the malware teardown, and a full post-root setup.

**Who it's for:** owners of an **H726** HY300-class projector who want to understand and control it - not just follow steps. Everything is specific to the **H726 (sun50iw15 / FEL `0x1911`)**; H713 owners are redirected below.

## Results - what was achieved

| Area | Outcome |
|---|---|
| **Root** | Achieved on **H726 / sun50iw15** - to our knowledge the **first publicly documented** root of this SoC in an HY300-class projector (all existing tooling targets H713). `adb shell su` returns `uid=0(root) … u:r:magisk:s0`. The enabling discovery was fixing the per-partition **`add_sum` verify-file checksum** so PhoenixSuit accepts a minimal-diff Magisk image. → [docs/02](docs/02-rooting.md) |
| **Auto-keystone** | **Fixed definitively.** The on-screen skew was root-caused not to any property but to **device orientation + projection mode**; after a one-time calibration the image **re-squares at any tilt and survives reboot**. → [docs/03](docs/03-keystone-display.md) |
| **Reverse-engineering** | The full **keystone / auto-keystone / autofocus pipeline** is mapped - two separate keystone paths, the gravity-sensor **z-axis sign gotcha**, the real baseline store (`set_angle_offset` → `/oem/gsensor_data`) - plus a complete **H726-vs-H713 difference table**. First public RE of this subsystem for H726. → [docs/03](docs/03-keystone-display.md) |
| **Security** | **BadBox 2.0 components removed** (fake `com.android.vending`/Tubesky, OTA droppers, telemetry) and the **`/system/xbin/hotacksu` setuid-root backdoor neutralized** and kept down across reboots via a Magisk module. → [docs/04](docs/04-debloat-security.md) |
| **Customization** | A rebuilt **home screen** (live weather, a second time zone, Vietnamese lunar date, an auto-changing photo-frame screensaver, a three-column header), a consolidated **"Hub" settings app**, a custom boot logo, and timezone/labels. → [docs/05](docs/05-post-root.md) |

Each result is written up with the **mistakes that led to the fix**, so the reasoning is reproducible, not just the recipe.

## What's inside

- **[`docs/`](docs)** - the technical write-ups (English; Vietnamese RE notes in [`docs/vi/`](docs/vi)):
  [hardware ID](docs/01-hardware-id.md) · [rooting + the `add_sum` fix](docs/02-rooting.md) · [keystone/display RE](docs/03-keystone-display.md) · [debloat & BadBox removal](docs/04-debloat-security.md) · [post-root customization](docs/05-post-root.md).
- **[`scripts/`](scripts)** - the rooted-image build script (`build_rooted_minimal.py`), SoC-analysis helpers, and the debloat-restore script.
- **[`launcher-mods/`](launcher-mods)** - a patch (and copies) of the custom home screen, on top of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
- **[`hub-app/`](hub-app)** - source of `com.hy300.hub`, the consolidated settings/keystone/casting launcher.
- **[`magisk-module/`](magisk-module)** - `hy300tweaks`: neutralize the backdoor + force the custom launcher as HOME.
- **[`assets/`](assets)** - boot logo and screenshots.

> **Not included (by design):** the ~2.2 GB rooted image, the stock firmware, and any proprietary APK / `.so` / vendor binary. GitHub can't host them and they aren't ours to redistribute - the build script reproduces the image from stock firmware instead, and every binary is linked to its source. See [License](#license).

## Compatibility

**Is this your device?**


| | This repo (**H726**) | Not this repo (**H713**) |
|---|---|---|
| SoC | `sun50iw15p1` (H726) | `sun50iw12` (H713) |
| FEL SoC id | **`0x1911`** | `0x1860` |
| Display pipeline | DRM/KMS direct (no MIPS) | MIPS co-processor (`display.bin`) |
| Gravity sensor | MPU-6880 | SC7A20 |

**Check yours:** `adb shell getprop ro.hardware` should print `sun50iw15p1`; in FEL mode `sunxi-fel version` should print `soc=00001911`. Full method: [docs/01](docs/01-hardware-id.md).

If you see `sun50iw12` / `0x1860`, this is an **H713** - use the projects this work is built on instead: [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), and the [H713 XDA thread](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/).

## Disclaimer

> **Use at your own risk.** Flashing an Allwinner device can **brick** it; you are responsible for your own hardware, and there is no warranty. This is for the **H726 only** (see Compatibility). The stock firmware is a white-label clone historically tied to the **BigDroidOS / Bigpanzi** family and flagged with **BadBox 2.0**; this repo shows how that was found and neutralized, but a cloned firmware can never be fully trusted - **do not sign into personal, banking or Google accounts on it, and keep it network-isolated.** No proprietary firmware, APKs or vendor libraries are redistributed here.

## Getting started

1. [Hardware ID](docs/01-hardware-id.md) - confirm your chip; learn the device aliases and how to enter FEL.
2. [Rooting](docs/02-rooting.md) - FEL + PhoenixSuit + the `add_sum` verify-file fix; build your image with [`scripts/build_rooted_minimal.py`](scripts/build_rooted_minimal.py).
3. [Debloat & security](docs/04-debloat-security.md) - remove bloat and neutralize the backdoor ([`magisk-module/hy300tweaks`](magisk-module/hy300tweaks)).
4. [Keystone & display](docs/03-keystone-display.md) - fix auto-keystone (orientation + projection mode + calibration).
5. [Post-root customization](docs/05-post-root.md) - the custom launcher ([`launcher-mods/`](launcher-mods)) and Hub app ([`hub-app/`](hub-app)).

## Acknowledgments

This work builds on, and is grateful to:

- the **XDA H726 rooting guide** - [*Rooting tutorial for a Magcubic HY300 Pro (Allwinner H726, Android 14)*](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/);
- the **H713 research foundation** - [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), well0nez's sunxi-tools / Linux port and keystone model, [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root);
- the **BadBox removal reference** - [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal);
- and **[Claude](https://www.anthropic.com/claude) (Anthropic's AI), used as a research assistant** throughout.

The new work here - the `add_sum` root path, the keystone/display reverse-engineering, the BadBox teardown and the post-root setup - was developed on top of those. Claude assisted as a tool; it is **not** a git co-author. The human (repository owner) made the decisions, took the risks, and ran the hardware.

## Lessons - mistakes that taught the method

- **Hex-editing the IMAGEWTY in place fails** - the container keeps per-partition `add_sum` checksums; swap a payload and you must recompute its verify-file, or PhoenixSuit rejects the flash. *This was the key to root.*
- **There are two keystone paths** - auto (gsensor → display engine, never touches `persist.htc.keystone.*`) and manual (the `htc.keystone` props). Writing manual values while auto is on gets overwritten - why they only "sometimes" stuck.
- **The "inverted trapezoid" was orientation, not a prop** - mounting the cylinder logo-down (gsensor `z < 0`) forces the firmware's "ceiling" branch, which inverts the correction and refuses a baseline. Logo-up (`z > 0`) just works.
- **`persist.sys.panelvalue` is only a UI cache** - `setprop` + reboot does not flip the image; you must call `factorySetPanelValue`.
- **Magisk magic-mount is broken on this firmware** - `system/` overlays don't apply; use `/oem`, direct partition writes or `service.d`.

## Repository layout

```
docs/            Technical write-ups (English; Vietnamese originals in docs/vi/)
scripts/         Rooted-image build + SoC analysis helpers (Python) + restore (PowerShell)
launcher-mods/   Patch + files for the custom home screen (on top of Gathin23/cleanyourtv)
hub-app/         "Hub" app source (com.hy300.hub)
magisk-module/   hy300tweaks - neutralize the hotacksu backdoor + force the launcher as HOME
assets/          Boot logo + screenshots
```

## References

**Guides** - [H726 root (XDA)](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/) · [H713 root (XDA)](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/)

**H713 research (foundation)** - [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify) · [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research) · [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root) · [schaumann-byte/hy300-projector-homeassistant](https://github.com/schaumann-byte/hy300-projector-homeassistant) · well0nez (sunxi-tools H713 fork + `allwinner-h713-linux`, keystone model)

**Security** - [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal)

**Tools** - [Magisk](https://github.com/topjohnwu/Magisk) · [sunxi-tools](https://github.com/linux-sunxi/sunxi-tools) · [OpenixSuit](https://github.com/YuzukiTsuru/OpenixSuit) · [imagewty-tool](https://github.com/uictorius/imagewty-tool) · [MagiskPatcher](https://circlecashteam.github.io/MagiskPatcher/) · [jadx](https://github.com/skylot/jadx) (Apache-2.0) · PhoenixSuit (Allwinner)

**Post-root software** - [cleanyourtv launcher](https://github.com/Gathin23/cleanyourtv) (MIT) · [SmartTube](https://github.com/yuliskov/SmartTube) · [Material Files](https://github.com/zhanghai/MaterialFiles) · Aurora Store · [Lucide icons](https://github.com/lucide-icons/lucide) · [Open-Meteo](https://open-meteo.com) · Outfit font (OFL)

**Firmware** (linked, not redistributed) - androidpctv.com · magcubic-support.com. Wallpaper photos shipped by the launcher theme are CC-BY / CC-BY-SA / CC0 by their authors; see the theme's `CREDITS.md` upstream.

## License

[MIT](LICENSE) for the original work in this repository. It does **not** cover the upstream projects (each keeps its own license) or any proprietary vendor firmware / APKs / libraries, which are **not** included here. See the scope note in [LICENSE](LICENSE).

*Not affiliated with Allwinner, Magcubic, Gojodoq, Google or Anthropic.*
