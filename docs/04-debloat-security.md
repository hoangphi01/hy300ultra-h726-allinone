# 04. Debloat & security (BadBox 2.0 teardown)

Removing the BadBox-family malware baked into the stock firmware and masking the setuid-root backdoor, then trimming the bloat. Reference: [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal).

## 1. The threat

The stock firmware is a white-label clone historically tied to **BigDroidOS / Bigpanzi** and flagged with **BadBox 2.0**: residential-proxy (SOAX), telemetry, silent install, OTA self-reinstall. It is **firmware-resident, so it survives a factory reset**. Root lets you neutralize the parts that run; a cloned firmware still cannot be fully trusted.

## 2. Malware found & neutralized

| Component | What it is | Action |
|---|---|---|
| `com.htc.storeos` | `StoreOs_QZ` dropper | `pm uninstall --user 0` |
| `com.htc.htcotaupdate` | OTA that reinstalls removed junk | `pm uninstall --user 0` |
| `com.android.vending` | fake Play Store = **Tubesky** (`/product/priv-app/Tubesky`, signer `F0:FD:6C:5B...`, not Google) | `pm uninstall --user 0`; use **Aurora Store** for real Play apps |
| `/system/xbin/hotacksu` | **setuid-root `su` backdoor** (`-rwsr-sr-x root shell`) | mask it, see Section 3 |

## 3. Neutralizing the `hotacksu` backdoor

### Why not just remount `/system` read/write and delete it?

On an older ext4 `/system` that is exactly the right move, and it is the first thing most people try. It does not work here. On this Android 14 build `/system` lives inside `/` on a **device-mapper logical partition** (dynamic partitions packed into `super`), mounted ext4 **read-only at the block layer**. The remount is refused by the block device itself, before the filesystem is even consulted:

```sh
# getprop ro.hardware
sun50iw15p1
# grep ' / ' /proc/mounts
/dev/block/dm-0 / ext4 ro,seclabel,nodev,relatime 0 0
# mount -o rw,remount /
'/dev/block/dm-0' is read-only
```

Even with verity disabled for boot, the logical partitions in `super` are exposed read-only by `dm`, so `mount -o rw,remount` fails. To actually `rm` the file you would have to make that logical partition writable (overlayfs / `adb remount`, which needs spare space in `super`), and note `dm-0` also backs every `/apex` mount, so it is the whole system+apex image you would be touching. That is far more invasive and fragile than a systemless mask.

### The systemless mask

Instead **bind-mount an empty, non-executable file over it** (as root, in the global mount namespace, so the mask propagates to every process):

```sh
printf '' > /dev/.hk_block
chmod 000 /dev/.hk_block
mount -o bind /dev/.hk_block /system/xbin/hotacksu
```

After this, any process that tries to `exec` the backdoor gets *Permission denied*, and the entry reads as a 0-byte `000` file:

```sh
# ls -lZ /system/xbin/hotacksu
---------- 1 root root u:object_r:device:s0 0 /system/xbin/hotacksu
```

This is reversible (nothing on the real partition is ever written, so there is no chance of a bad write bricking boot) and keeps the AVB state clean. Make it **persistent across reboot** with the Magisk module in [`../magisk-module/hy300tweaks`](../magisk-module/hy300tweaks) (its `service.sh` does the bind-mount on boot).

Notes:
- `/vendor/etc/awbms_config` whitelists the `com.hotack` namespace from the task-killer (persistence). That same `hotack` namespace is used by the vendor for **both** keystone (`hotack.vaf.*`, `android.intent.hotack_keystone`) **and** the malware, so do not blanket-kill it.
- GMS is `PrebuiltGmsCorePano` (the standard Android-TV GMS build; likely legitimate).
- **Magisk magic-mount is broken on this firmware**: `system/` overlays do not apply. Use `/oem`, direct partition writes, or `/data/adb/service.d` instead. That is why the backdoor mask is a bind-mount, not a module overlay.

## 4. Debloat list (reversible)

Removed with `pm uninstall --user 0` (restore with `cmd package install-existing <pkg>`, or run [`../scripts/restore-projector.ps1`](../scripts/restore-projector.ps1)):

```
com.android.toofifi              HanYing H713 adware (suspected Bigpanzi)
com.toofifi.lineserver           USBCastServer, same toofifi namespace
com.htc.eventuploadservice       telemetry
com.htc.storeos                  vendor store / dropper
com.htc.htcotaupdate             OTA reinstaller
com.softwinner.update            Allwinner OTA
android.rockchip.update.service  OTA
com.mobisystems.office           Office
com.android.music                Music
com.softwinner.TvdVideo          video player
com.softwinner.awlivetv          Live TV
```

Keep (do not remove): `com.hysd.vafocus` (keystone/autofocus, see [03. Keystone & display](03-keystone-display.md)), and the casting apps (`com.mphotool.airplay`, `com.mphotool.dlna`, `com.htc.samescreen`, `com.softwinner.miracastReceiver`).

## 5. Hardening

- Do **not** sign into personal / Google / banking accounts on this device.
- **Network-isolate it** (guest Wi-Fi / IoT VLAN).
- **System-wide ad/tracker block:** set Private DNS to `dns.adguard.com` (Settings -> Network -> Private DNS). This also blunts any surviving telemetry endpoints.
