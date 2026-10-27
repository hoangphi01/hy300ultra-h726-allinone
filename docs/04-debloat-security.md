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

### You cannot remove it on the live device

On an older ext4 `/system` you would remount read/write and `rm` the file. That does not work here. On this Android 14 build `/system` lives inside `/` on a **device-mapper logical partition** (dynamic partitions packed into `super`), mounted ext4 **read-only at the block layer**. The remount is refused by the block device itself, before the filesystem is even consulted:

```sh
# getprop ro.hardware
sun50iw15p1
# grep ' / ' /proc/mounts
/dev/block/dm-0 / ext4 ro,seclabel,nodev,relatime 0 0
# mount -o rw,remount /
'/dev/block/dm-0' is read-only
```

Even with verity disabled for boot, the logical partitions in `super` are exposed read-only by `dm`, and `dm-0` also backs every `/apex` mount, so there is no live partition to edit in place.

### Two valid approaches

The backdoor is only useful to a non-root process: a normal app execs the setuid `hotacksu` to become root. Both approaches below close that path; they differ in where the risk sits, so the choice is a matter of preference.

**A. Systemless mask (bind-mount).** Cover the file with an empty, non-executable file, as root, in the global mount namespace, so the mask propagates to every process:

```sh
printf '' > /dev/.hk_block
chmod 000 /dev/.hk_block
mount -o bind /dev/.hk_block /system/xbin/hotacksu
```

Any process that then tries to `exec` the backdoor gets *Permission denied*, and the entry reads as a 0-byte `000` file:

```sh
# ls -lZ /system/xbin/hotacksu
---------- 1 root root u:object_r:device:s0 0 /system/xbin/hotacksu
```

Make it persistent with the Magisk module in [`../magisk-module/hy300tweaks`](../magisk-module/hy300tweaks); its `service.sh` re-applies the bind-mount on boot.

**B. Offline removal and reflash.** Extract the system image, mount it read/write on a PC, delete the file, then repack and reflash. Because `/system` is a logical partition inside `super` and the Allwinner `IMAGEWTY` keeps a per-partition `add_sum` verify-file, this means repacking `super`, keeping `vbmeta` verity-disabled, and recomputing the verify-file, or PhoenixSuit rejects the whole image with "firmware file is valid" (see [02. Rooting](02-rooting.md)).

### Choosing between them

| | A. Systemless mask | B. Offline delete + reflash |
|---|---|---|
| End state | file masked (bytes remain underneath) | file physically removed |
| Writes to the partition | none | yes (repack + reflash) |
| Reversible | yes (remove the module) | no |
| Flash required | no | yes (`super` + `vbmeta`, over FEL) |
| If it goes wrong | device still boots | non-booting until recovered by reflashing stock over FEL |
| Extra work | none | repack `super`, keep verity off, recompute `add_sum` |
| Holds without Magisk | no (re-applied each boot) | yes |

Within the threat model the two are equivalent. Reaching the bytes under the mask already requires root, at which point the backdoor is moot, so approach A leaves no usable escalation path; it is the standard systemless model (nothing is written to the partition, so it cannot brick and is fully reversible). Approach B removes the file permanently at the cost of a real flash path. On a unit that is already rooted and configured, approach A avoids redoing root and customization to erase a file that already cannot execute.

> Either way this is a cloned firmware. Masking or removing one file does not make it trustworthy: keep the device network-isolated and do not sign into personal accounts (Section 5).

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
