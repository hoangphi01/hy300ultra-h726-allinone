# 02. Rooting the H726 (the `add_sum` verify-file method)

Getting a Magisk root onto a chip (`sun50iw15` / FEL `0x1911`) that no public tooling supports. The route that works: flash a minimal-diff rooted IMAGEWTY via PhoenixSuit over FEL, and make PhoenixSuit accept it by fixing the per-partition verify-file checksums.

> Warning: flashing can brick the device. Confirm `soc=00001911` first ([01. Hardware ID](01-hardware-id.md)).

## 1. The problem

The first instinct, **hex-editing the stock `IMAGEWTY` in place** (patching `init_boot`, `vbmeta`, `dlinfo`, `sys_partition`), fails: PhoenixSuit rejects it with a *"firmware file is valid"* error. The container keeps per-partition checksums, and a surgical byte edit invalidates them. The fix is to **swap whole partition payloads and recompute their checksums** instead of editing in place.

## 2. The checksum (`add_sum`)

- Each partition's data is followed by a tiny **verify file `V<name>.fex`**: 16 bytes = one `u32` little-endian checksum plus zero padding.
- The algorithm, `add_sum`: pad the payload to a multiple of 4 bytes, then sum every `u32` (little-endian) modulo 2^32.
- The `dlinfo` table entry (72 bytes per partition) points at the V-file via `vf_filename` at `+0x30`, and carries `verify_flag = 1` at `+0x44`.
- On this unit (offsets are unit/build-specific; the scripts compute them, do not hardcode):
  - `init_boot_a` payload at `0x1120dc50`; its `Vinit_boot.fex` at `init_boot_off + 0x800000`.
  - `vbmeta_a` payload at `0x82443d70`; its `Vvbmeta.fex` at `vbmeta_off + 0x2000`.

If a payload no longer matches its V-file checksum, PhoenixSuit refuses the image. Recompute the V-file and it accepts.

## 3. Building the rooted image

Use [`../scripts/build_rooted_minimal.py`](../scripts/build_rooted_minimal.py). It:

1. Copies the stock Magcubic `IMAGEWTY` **byte-for-byte**.
2. Overwrites **only two payloads**, at their original offsets, same size or smaller (zero-padded):
   - `init_boot_a` with a **Magisk-patched `init_boot`** (`init_boot` is where Magisk lives on Android 14).
   - `vbmeta_a` with a **verification-disabled `vbmeta`**.
3. Recomputes `add_sum` for **those two V-files only**. Everything else (`dlinfo`, `sys_partition`, all other partitions) stays identical to stock.

```sh
python scripts/build_rooted_minimal.py \
    --stock    "Magcubic HY300 Ultra Allwinner H726.img" \
    --initboot init_boot_magisk.img \
    --vbmeta   vbmeta_disabled.fex \
    --out      HY300Ultra_H726_ROOTED.img
```

The two inputs:
- `init_boot_magisk.img`: extract `init_boot_a` from the stock image, patch it with the **Magisk app** on another phone (or the online **MagiskPatcher**: armeabi-v7a, with *Keep Verify / Keep Force Encrypt / Patch Vbmeta Flag / Legacy SAR*).
- `vbmeta_disabled.fex`: a `vbmeta` with the verification flags disabled.

> Calibration is safe. Allwinner stock flashing **skips the private/reserve partition**, so per-unit colour/keystone calibration survives a full reflash; a properly repacked rooted image is safe for it too. The fragile surgical / dlinfo-trim approach is not needed.

## 4. Flashing

1. Enter FEL: `adb reboot efex`, or the hardware switch under the HDMI port + power (more robust). A device with a broken boot **auto-enters FEL**; it is not bricked, the BootROM FEL lives in mask ROM.
2. Use **PhoenixSuit V1.19** with the Allwinner `usbdrv.inf` driver (not the usbipd / VirtualBox driver). Load `HY300Ultra_H726_ROOTED.img`, click *Upgrade*, confirm *"Do Mandatory Upgrade?"*, then replug USB / power to trigger. PhoenixSuit catches FEL only at the enumeration moment.
3. It boots slot `_a` with `veritymode=disabled`, `verifiedbootstate=orange`.

> FEL DRAM init and U-Boot exec work with the **stock `fes1.fex`** extracted from the firmware (loads to SRAM `0x0010C000`); the vendor U-Boot has an ARM32 self-switching head, so no external RVBAR jump is needed (both H713 RVBAR addresses hang on H726).

## 5. Finishing root (the Magisk app)

The patched `init_boot` gives the Magisk core but not the manager app:

```sh
adb install Magisk-v31.1.apk         # com.topjohnwu.magisk
```
Then in the Magisk app: **Settings -> Superuser access = "Apps and ADB"**, **Automatic response = Grant**, and allow the **"Shell"** (adb, uid 2000) entry. Verify:

```sh
adb shell su -c id                   # uid=0(root) ... context=u:r:magisk:s0
```

## 6. SoC-analysis helpers

For porting to another sun50iw15 unit: `scripts/find_fes1_addr.py`, `find_rmr.py` (needs `capstone`), `scan_rvbar.py` are the low-level helpers used to work out the FEL / U-Boot addresses. `edit_syspart.py` limits which partitions `sys_partition.fex` will flash. `build_rooted.py` is a more surgical variant kept for reference; prefer `build_rooted_minimal.py`.

The XDA H726 guide's alternative flow (`imagewty-tool` + online MagiskPatcher, full `update_new.img`), linked in the [README](../README.md#8-acknowledgments--references), automates the checksum step with a different tool.
