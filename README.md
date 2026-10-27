<div align="center">

# HY300 Ultra — Allwinner H726 — All-in-One

**Root · reverse-engineering · debloat/security · post-root customization**
for the **Gojodoq / Magcubic HY300 Ultra** projector (Allwinner **H726 / sun50iw15**).

🇬🇧 **English** · [🇻🇳 Tiếng Việt](README.vi.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![SoC](https://img.shields.io/badge/SoC-Allwinner%20H726%20(sun50iw15)-blue)
![FEL ID](https://img.shields.io/badge/FEL%20ID-0x1911-informational)
![Android](https://img.shields.io/badge/Android-14-green)
![Root](https://img.shields.io/badge/root-Magisk-success)
![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)
![Affiliation](https://img.shields.io/badge/affiliation-none-lightgrey)

<img src="assets/screenshots/home-3col.png" width="640" alt="Custom home screen">

</div>

---

## ⚠️ Disclaimer — read first

- **Use at your own risk.** Flashing an Allwinner device can **brick** it. You are responsible for your own hardware. No warranty.
- **This is for ONE chip only: Allwinner `H726` = `sun50iw15` = FEL SoC id `0x1911`.** Retail names on this same board include **Gojodoq / Magcubic HY300 Ultra / Pro / Plus / MAX**, firmware model **`ADT-3`**, SKU **`AC1076`**.
- **If your projector is an `H713` (`sun50iw12`, FEL id `0x1860`) — this repo is NOT for you.** Go instead to the H713 work this project was built on: [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), and the [H713 XDA thread](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/). See [how to check your chip](docs/01-hardware-id.md).
- **The stock firmware on these units is a white-label clone historically tied to the BigDroidOS / Bigpanzi family and flagged with BadBox 2.0** (residential-proxy / telemetry / OTA self-reinstall, plus a `setuid`-root backdoor). This repo documents how that was found and neutralized, but a cloned firmware can never be fully trusted — **do not sign into personal / banking / Google accounts on it; network-isolate it.**
- **No proprietary firmware, APKs, or vendor `.so` libraries are redistributed here.** Only original work (scripts, docs, the launcher patch, the Hub app) is included; everything else is linked to its source.

---

## 🙏 Acknowledgments

This repository stands on the shoulders of others. Building on:

- the **XDA H726 rooting guide** — [*Rooting tutorial for a Magcubic HY300 Pro (Allwinner H726, Android 14)*](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/),
- the **H713 research foundation** — [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), well0nez's sunxi-tools/Linux port + keystone model, [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root),
- the **BadBox removal reference** — [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal),
- and with **[Claude](https://www.anthropic.com/claude) (Anthropic's AI) as a research assistant**,

…I developed and documented what the existing guides did not cover:

1. **Root of the H726 / sun50iw15** by fixing the per-partition **`add_sum` verify-file checksum** so PhoenixSuit accepts a minimal-diff Magisk image (the hex-edit approach everyone hits first *breaks* the IMAGEWTY checksums). → [`docs/02-rooting.md`](docs/02-rooting.md)
2. A full **reverse-engineering of the keystone / auto-keystone / autofocus subsystem on H726** — the *two* separate keystone paths, the gravity-sensor **z-axis sign gotcha** that inverts auto-correction, and the real baseline mechanism (`PrjScreen.set_angle_offset()` → `/oem/gsensor_data`). Includes an **H726-vs-H713 difference table**. → [`docs/03-keystone-display.md`](docs/03-keystone-display.md)
3. **Removal of BadBox 2.0**, including the `/system/xbin/hotacksu` setuid-root backdoor and a fake `com.android.vending` (Tubesky). → [`docs/04-debloat-security.md`](docs/04-debloat-security.md)
4. A complete **post-root customization** set (launcher with weather / dual clock / lunar calendar / photo-frame screensaver, a consolidated "Hub" app, boot logo, timezone). → [`docs/05-post-root.md`](docs/05-post-root.md)

> Claude helped as a tool; it is **not** credited as a git co-author. The human (repository owner) made the decisions, took the risks, and ran the hardware.

---

## 📟 Is this your device?

| | ✅ This repo (**H726**) | ❌ Not this repo (**H713**) |
|---|---|---|
| SoC | `sun50iw15p1` (H726) | `sun50iw12` (H713) |
| FEL SoC id | **`0x1911`** | `0x1860` |
| Display pipeline | DRM/KMS direct (no MIPS) | MIPS co-proc (`display.bin`) |
| gsensor | MPU-6880 | SC7A20 |
| Where to go | **here** | [H713 projects →](#-is-your-device) see Disclaimer |

**Check yours:** `adb shell getprop ro.hardware` (expect `sun50iw15p1`), or in FEL mode `sunxi-fel version` (expect `soc=00001911`). Full method in [`docs/01-hardware-id.md`](docs/01-hardware-id.md).

---

## 🧭 Step by step

1. **[Hardware ID](docs/01-hardware-id.md)** — confirm your chip, device aliases, FEL entry.
2. **[Rooting](docs/02-rooting.md)** — FEL + PhoenixSuit + the `add_sum` verify-file fix; build your image with [`scripts/build_rooted_minimal.py`](scripts/build_rooted_minimal.py).
3. **[Debloat & security](docs/04-debloat-security.md)** — remove bloat, neutralize the BadBox backdoor (see [`magisk-module/hy300tweaks`](magisk-module/hy300tweaks)).
4. **[Keystone & display](docs/03-keystone-display.md)** — fix auto-keystone definitively (orientation + projection mode + calibration).
5. **[Post-root customization](docs/05-post-root.md)** — the custom launcher ([`launcher-mods/`](launcher-mods)) and Hub app ([`hub-app/`](hub-app)).

> **The rooted image itself (~2.2 GB) is not hosted here** — GitHub can't, and the base firmware is proprietary. The build script reproduces it from the stock firmware instead.

---

## 🧩 The journey — mistakes that taught the method

Real work is messy. These are the dead-ends, in case they save you time:

- **Hex-editing the IMAGEWTY in place → PhoenixSuit rejects it.** The container has per-partition `V<name>.fex` checksum files (`add_sum`: pad to ×4, sum of `u32` LE). Change a payload and you must recompute its V-file, or the flash is refused. *This was the key to root.*
- **Setting the wrong keystone prop.** `persist.display.keystone_*` is the "accepted" copy; the live manual path is `persist.htc.keystone.*`; and **auto-keystone doesn't use either** — it applies through `KeystoneUtils_726` → the display engine. Two separate paths, which is why manual values "sometimes" stuck.
- **The "inverted trapezoid."** Root cause was never a prop — it was **mounting the cylinder logo-down (gsensor z < 0)**, which pushes the firmware into its "ceiling" branch (it inverts the correction and refuses to save a baseline). Flip it logo-up (z > 0) and it just works.
- **`persist.sys.panelvalue` is only a UI cache** — `setprop` + reboot does *not* flip the image; you must call `factorySetPanelValue` via the Projection-Mode screen.
- **Magisk magic-mount is broken on this firmware** — `system/` overlays don't apply; use `/oem`, direct partition writes, or `service.d`.

Full write-ups with the fixes live in [`docs/`](docs).

---

## 📂 Repo layout

```
docs/            Technical write-ups (English; Vietnamese originals in docs/vi/)
scripts/         Rooted-image build scripts + SoC analysis helpers (Python) + restore (PowerShell)
launcher-mods/   Patch + files for the custom home screen (on top of Gathin23/cleanyourtv)
hub-app/         "Hub" app source (com.hy300.hub) — consolidated settings/keystone/casting launcher
magisk-module/   hy300tweaks — neutralize the hotacksu backdoor + force the launcher as HOME
assets/          Boot logo + screenshots
```

---

## 📚 References & citations

**Guides**
- H726 root (XDA): https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/
- H713 root (XDA, the basis): https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/

**H713 research (technical foundation)**
- https://github.com/probonopd/hy310-deshittify
- https://github.com/YuujiLab/HY300-H713-Research
- https://github.com/gniax/magcubic-hy300-root
- https://github.com/schaumann-byte/hy300-projector-homeassistant
- well0nez — sunxi-tools (H713 fork) + `allwinner-h713-linux` (the keystone RE model used as a 1:1 reference)

**Security**
- https://github.com/maximilian-sh/hy350max-badbox-removal

**Tools**
- Magisk — https://github.com/topjohnwu/Magisk
- sunxi-tools — https://github.com/linux-sunxi/sunxi-tools
- OpenixSuit — https://github.com/YuzukiTsuru/OpenixSuit
- imagewty-tool — https://github.com/uictorius/imagewty-tool
- MagiskPatcher (online) — https://circlecashteam.github.io/MagiskPatcher/
- jadx — https://github.com/skylot/jadx *(Apache-2.0)*
- PhoenixSuit — Allwinner flashing tool

**Post-root software**
- Launcher: https://github.com/Gathin23/cleanyourtv *(MIT — the home screen here is a patch on top)*
- SmartTube — https://github.com/yuliskov/SmartTube
- Material Files — https://github.com/zhanghai/MaterialFiles
- Aurora Store · Lucide icons (https://github.com/lucide-icons/lucide) · Open-Meteo API (https://open-meteo.com) · Outfit font *(OFL)*

**Firmware** *(linked, not redistributed)* — androidpctv.com · magcubic-support.com

Wallpaper photos shipped by the launcher theme are **CC-BY / CC-BY-SA / CC0** by their authors; see the theme's `CREDITS.md` in the upstream launcher repo.

---

## 📄 License

[MIT](LICENSE) for the original work in this repo. It does **not** cover upstream projects (each keeps its own license) or any proprietary vendor firmware/APKs/libraries, which are **not** included here. See the [LICENSE](LICENSE) scope note.

*Not affiliated with Allwinner, Magcubic, Gojodoq, Google, or Anthropic.*
