# 01 - Hardware ID & device identity

> Vietnamese original of the RE notes: [`vi/keystone-h726.md`](vi/keystone-h726.md). This page is the hardware fingerprint you need **before** doing anything else.

## There is no single "model"

This is a **white-label reference design** sold under many brands. All of the following are the **same H726 board**:

| Where it shows up | Name |
|---|---|
| Retail label (as purchased) | **Gojodoq HY300 Ultra (AC1076)** |
| True hardware | **HY300 Ultra · Allwinner H726 = `sun50iw15p1`** |
| Running firmware (Magcubic build) | `ro.product.model = ADT-3`, `ro.product.device = adt3`, Android **14**, `ro.build.display.id = Projector_20250806.111734` |
| Other aliases on the same board | HY300 Plus / HY300 MAX / Magcubic HY300 Pro / Ultra |

## Chip / SoC identifiers

| Property | Value |
|---|---|
| SoC | **Allwinner H726 = `sun50iw15` (`sun50iw15p1`)**, ARM Cortex-A53 |
| Userspace ABI | **`armeabi-v7a` (32-bit)** - this matters: many apps / 64-bit APKs won't install |
| RAM / panel | ~1 GB (low-RAM) / 720p (1280×720) |
| **FEL SoC id** | **`0x1911`** (sunxi-fel reports `AWUSBFEX soc=00001911(unknown)`) |
| FEL USB VID:PID | **`1f3a:efe8`** |
| FEL scratchpad | `0x00127a00` |
| fes1 load addr | `0x0010C000` · u-boot load `0x4A000000` · DRAM base `0x40000000` |
| gsensor | **MPU-6880** |
| Display | **No MIPS co-processor** → DRM/KMS direct (`/dev/sunxi_drm_heap`, `/dev/sunxi_soc_info`) |
| Boot | A/B slots, `init_boot`, `ro.boot.verifiedbootstate=orange`, SELinux **Permissive**, `veritymode=disabled` |

## How to check YOUR chip (do this first)

**Over ADB (booted):**
```sh
adb shell getprop ro.hardware       # expect: sun50iw15p1   (H713 would be sun50iw12)
adb shell getprop ro.product.cpu.abi # expect: armeabi-v7a   (32-bit)
```

**Over FEL (powered into FEL mode, USB to PC):**
```sh
sunxi-fel version                    # expect: soc=00001911   (H713 would be 00001860)
```

If you see `sun50iw12` / `0x1860`, **stop** - this is an **H713**, not an H726. Use the H713 projects linked in the [README](../README.md#compatibility).

## Entering FEL (download mode)

- **`adb reboot efex`** worked reliably (soft entry).
- Hardware entry: a **recessed switch under the HDMI port** (press with a toothpick) + the power button - per the XDA H726 guide; more robust for unbricking than the soft path.
- ⚠️ **`adb reboot bootloader` boots into RECOVERY**, not fastboot - and "Reboot to bootloader"/Home there can trigger a **factory wipe**. Avoid it; use FEL.

## Why H726 ≠ H713 (summary)

All existing community tooling (sunxi-fel, U-Boot, the H713 root guides) targets **H713 / `0x1860`**. The H726 is a newer SoC (`0x1911`) with **no public BSP, sunxi-fel port, or U-Boot** at the time of writing - which is why rooting it required the custom `add_sum` approach in [`02-rooting.md`](02-rooting.md), and why the display stack differs (see [`03-keystone-display.md`](03-keystone-display.md)).
