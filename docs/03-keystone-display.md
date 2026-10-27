# 03. Keystone / auto-keystone / display subsystem (reverse-engineering)

How auto-keystone really works on the H726, why it "goes wrong", and the definitive fix (verified, survives reboot). Method: `strings` and jadx of `MagcubicOS.apk` and `tpjz_allwinner.apk` (`com.hysd.vafocus`), device probing, and the H713 keystone model (`allwinner-h713-linux/.../h713-keystone/model/*.py`) as a near-1:1 reference. Vietnamese original: [`vi/keystone-h726.md`](vi/keystone-h726.md).

## 1. Quick fix

The skew ("inverted trapezoid") is **not** a keystone-property problem; it is **device orientation + projection mode**. For a cylinder-on-gimbal unit that can reach "horizontal" two ways:

1. **Mount it logo-up** so the gravity sensor reads `z > 0` (`cat /sys/devices/platform/prj343x/xyz_data` gives a positive z, around `+8192`, i.e. ~1 g, when lying flat and upright). Logo-down (`z < 0`) throws the firmware into its "ceiling" branch, which inverts the correction and refuses to save a baseline.
2. **Fix the image flip:** `am start -n com.htc.magcubicos/.activity.ProjectActivity`, pick the mode where text is upright (desk, logo-up = mode `0` Front). This calls `factorySetPanelValue(MIRROR, ...)`. `persist.sys.panelvalue` is only a UI cache; `setprop` + reboot does not flip.
3. **Calibrate:** `am start -n com.htc.magcubicos/.activity.InitAngleActivity`, hold level and still, press OK. `PrjScreen.set_angle_offset()` captures the flat reference and writes the flag `/oem/gsensor_data` (persistent, survives reboot).
4. **Enable auto:** `setprop persist.sys.tpryauto 1` + `am broadcast -a android.intent.hotack_keystone --ei ratio 1 --ei keystone 1`.

Result: level = square; tilt 15 degrees and `persist.sys.keystoneAngle` tracks it and the image re-squares; survives a power cycle.

## 2. Pipeline

```
gsensor (MPU-6880)  ->  raw counts x,y,z  (/sys/devices/platform/prj343x/xyz_data, "x,y,z")
   |
com.hysd.vafocus / LocalService.optKeystoneFun
   |   GsX,GsY = atan(counts) - atan(BASELINE)       (du = current angle - flat reference)
   |   if counts.z < 0 -> negate GsX,GsY             (the "ceiling/inverted" branch)
   v
libduRYXtp.so: getRYXRatiomapDate -> 8 corner insets
   v
KeystoneUtils_726  ->  DRM/KMS homography on the GPU (SurfaceFlinger), not a hardware warp
```

The correction is a **GPU homography** composited in SurfaceFlinger. There is **no display co-processor** on H726 (unlike H713's MIPS `display.bin`).

## 3. Two separate keystone paths

At a 15-degree tilt the auto-correction is clearly applied, yet `persist.htc.keystone.*` stays `0`. That proves there are two independent paths:

| Path | How it applies | Props |
|---|---|---|
| **AUTO** (gsensor) | `optKeystoneFun` -> `KeystoneUtils_726` -> DE/native | tracked via `persist.sys.keystoneAngle` / `keystonefinalAngle`; does not touch `persist.htc.keystone.*` |
| **MANUAL** (4-corner) | `CorrectionActivity` / `vafocus/.TchhdActivity` | writes `persist.htc.keystone.{ltx,lty,rtx,rty,lbx,lby,rbx,rby}` |

`persist.display.keystone_*` is the "accepted / clamp" copy. So writing `htc.keystone` **while auto is on** gets overwritten by the DE path, which is exactly why manual values only "sometimes" stuck.

## 4. Baseline mechanism

- Capture = `PrjScreen.set_angle_offset()` (a framework method, called by reflection from MagcubicOS; `get_angle_offset()` reads it back). It snapshots the current gsensor as the flat reference.
- Triggered by `InitAngleActivity` ("Please place the device on a horizontal surface and press OK, do not move").
- Persisted as the flag file `/oem/gsensor_data` (the `/oem` partition survives reboot).
- `checkGsensorDataOk` requires `z >= 0`, which is why the device must be logo-up when calibrating; `z < 0` is rejected.
- `Settings.Global/Secure "GsInitXYZ"` is **not** used on this unit (it stays `null`), an H726-vs-H713 difference.

## 5. Projection-mode / image-flip control

- Applied by `factorySetPanelValue(E_AW_PANEL_CONFIG_MIRROR, mode)` (HAL `AwTvDisplayManager` -> `ITvServer`), set from `ProjectActivity`; read back with `factoryGetPanelValue`.
- Modes: `0` = Front (desk), `1` = Rear, `2` = Front-ceiling (180-degree flip), `3` = Rear-ceiling.
- `persist.sys.panelvalue` is cache only.

## 6. Key properties

| Prop | Role |
|---|---|
| `persist.sys.tpryauto` | `1` = auto-keystone on |
| `persist.sys.keystoneAngle` / `keystonefinalAngle` | measured / applied tilt angle (auto path) |
| `persist.htc.keystone.*` | manual 4-corner path |
| `persist.display.keystone_*` | accepted / clamp copy |
| `persist.sys.panelvalue` | projection-mode cache (apply via `factorySetPanelValue`) |
| broadcasts | `android.intent.hotack_keystone`, `com.htc.DISPLAY_SETTINGS` |

## 7. H726 vs H713

| | H713 (sun50iw12) | H726 (sun50iw15) |
|---|---|---|
| FEL SoC id | `0x1860` | `0x1911` |
| Display co-proc | MIPS `display.bin` (`/dev/mipsloader`) | none; DRM/KMS direct (`/dev/sunxi_drm_heap`) |
| gsensor | SC7A20 | MPU-6880 (reports `z < 0` when inverted) |
| Baseline store | `GsInitXYZ` (Settings) | `PrjScreen.set_angle_offset()` -> `/oem/gsensor_data` (GsInitXYZ unused) |
| Keystone apply | prop / SF txn | two paths: auto -> `KeystoneUtils_726` -> DE; manual -> `persist.htc.keystone.*` |
| Image flip | - | `factorySetPanelValue(MIRROR, 0..3)` via `ProjectActivity` |
| Userspace / libs | arm64 + arm32 | 32-bit `armeabi-v7a`; `libduRYXtp.so` (geom) + `libPxScale.so` (zoom) |
| Keystone util class | - | dedicated `KeystoneUtils_726` |

## 8. Autofocus (separate from keystone)

UVC camera + stepper motor (`prj_config.ini`: `motor_type`, `motor_step_delay`); `VaFocus=10` hill-climb on sharpness. `vafocus/.TchjzActivity` is the camera auto-keystone (needs a flat, well-lit wall). Do **not** uninstall `com.hysd.vafocus`: it is the keystone/focus service, not adware.
