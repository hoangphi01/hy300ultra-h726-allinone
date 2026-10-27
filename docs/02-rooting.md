# 02 — Rooting the H726 (the `add_sum` verify-file method)

> **Why this is its own method:** the H726 (`sun50iw15` / FEL `0x1911`) is newer than the H713 (`0x1860`) that every public sunxi-fel / U-Boot / root guide targets. There is **no public BSP or FEL port** for it. The route that works is: flash a **minimal-diff rooted IMAGEWTY via PhoenixSuit over FEL**, and the one thing that makes PhoenixSuit *accept* that image is fixing the per-partition **verify-file checksums**. That checksum is the heart of this page.
>
> ⚠️ Flashing can brick the device. Read [`01-hardware-id.md`](01-hardware-id.md) and confirm `soc=00001911` first.

## The big realization

The first instinct — **hex-editing the stock `IMAGEWTY` in place** (patching `init_boot`, `vbmeta`, `dlinfo`, `sys_partition`) — **fails**: PhoenixSuit rejects it with *"firmware file is valid"* errors. The container keeps **per-partition checksums**, and a surgical byte edit invalidates them.

So instead of editing in place, you **swap whole partition payloads and recompute their checksums.**

## Anatomy of the checksum (`add_sum`)

- Each partition's data is followed by a tiny **verify file `V<name>.fex`** — **16 bytes = one `u32` little-endian checksum + zero padding**.
- The checksum algorithm, call it **`add_sum`**: pad the payload to a multiple of 4 bytes, then **sum every `u32` (little-endian), modulo 2³²**.
- The `dlinfo` table entry (72 bytes per partition) points at the V-file via `vf_filename` at offset `+0x30`, and has `verify_flag = 1` at `+0x44`.
- On this unit (offsets are unit/build-specific — the scripts compute them, don't hardcode):
  - `init_boot_a` payload @ `0x1120dc50`; its `Vinit_boot.fex` @ `init_boot_off + 0x800000`.
  - `vbmeta_a` payload @ `0x82443d70`; its `Vvbmeta.fex` @ `vbmeta_off + 0x2000`.

**If a payload no longer matches its V-file checksum, PhoenixSuit refuses the image.** Recompute the V-file and it accepts.

## Build the rooted image

Use [`../scripts/build_rooted_minimal.py`](../scripts/build_rooted_minimal.py). It:

1. Copies the stock Magcubic `IMAGEWTY` **byte-for-byte**.
2. Overwrites **only two payloads**, at their original offsets, same size or smaller (zero-padded):
   - `init_boot_a` → a **Magisk-patched `init_boot`** (`init_boot` is where Magisk lives on Android 14).
   - `vbmeta_a` → a **verity/verification-disabled `vbmeta`**.
3. Recomputes `add_sum` for **those two V-files only** and writes them back. Everything else (`dlinfo`, `sys_partition`, all other partitions) stays **identical to stock**.

```sh
python scripts/build_rooted_minimal.py \
    --stock  "Magcubic HY300 Ultra Allwinner H726.img" \
    --initboot init_boot_magisk.img \
    --vbmeta  vbmeta_disabled.fex \
    --out     HY300Ultra_H726_ROOTED.img
```

**Where the two inputs come from:**
- `init_boot_magisk.img`: extract `init_boot_a` from the stock image, patch it with the **Magisk app** on another phone (or the online **MagiskPatcher**, armeabi-v7a + *Keep Verify / Keep Force Encrypt / Patch Vbmeta Flag / Legacy SAR*).
- `vbmeta_disabled.fex`: a `vbmeta` with the verification flags disabled.

> **Calibration is safe.** Allwinner stock flashing **skips the private/reserve partition**, so per-unit color/keystone calibration survives a full reflash — a properly repacked rooted image is safe for calibration too. (The fragile surgical/dlinfo-trim approach is **not** needed.)

## Flash it

1. Put the device in FEL: `adb reboot efex`, or the hardware switch under the HDMI port + power (more robust). A device with a broken boot **auto-enters FEL** — it is *not* bricked; the BootROM FEL is in mask ROM.
2. **PhoenixSuit V1.19**, Allwinner `usbdrv.inf` driver (NOT usbipd/VirtualBox driver). Load `HY300Ultra_H726_ROOTED.img` → *Upgrade* → *"Do Mandatory Upgrade?"* → **OK**, then replug USB/power to trigger. PhoenixSuit only catches FEL at the enumeration moment.
3. It boots slot `_a` with `veritymode=disabled`, `verifiedbootstate=orange`.

> FEL DRAM init + U-Boot exec work using the **stock `fes1.fex`** extracted from the firmware (loads to SRAM `0x0010C000`); the vendor U-Boot has an ARM32 self-switching head, so no external rvbar jump is needed (both H713 rvbar addresses hang on H726).

## Finish root (the Magisk app)

The patched `init_boot` gives the Magisk **core** but not the manager app:

```sh
adb install Magisk-v31.1.apk        # com.topjohnwu.magisk
```
Then in the Magisk app: **Settings → Superuser access = "Apps and ADB"**, **Automatic response = Grant**, and allow the **"Shell"** (adb, uid 2000) entry. Now:

```sh
adb shell su -c id                  # uid=0(root) ... context=u:r:magisk:s0
```

## SoC-analysis helpers (if you're porting to another sun50iw15 unit)

`scripts/find_fes1_addr.py`, `find_rmr.py` (needs `capstone`), `scan_rvbar.py` — low-level helpers used while figuring out the FEL/U-Boot addresses. `edit_syspart.py` limits which partitions `sys_partition.fex` will flash. `build_rooted.py` is a more "surgical" variant kept for reference; **prefer `build_rooted_minimal.py`.**

See also the XDA H726 guide's alternative flow (`imagewty-tool` + online MagiskPatcher, full `update_new.img`) linked in the [README](../README.md#-references--citations) — it automates the checksum step with a different tool.
