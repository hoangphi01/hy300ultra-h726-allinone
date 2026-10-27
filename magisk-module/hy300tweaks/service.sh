#!/system/bin/sh
# (1) Mask the BadBox setuid-root backdoor /system/xbin/hotacksu (bind an empty 000 file).
if [ -u /system/xbin/hotacksu ]; then
  printf '' > /dev/.hk_block 2>/dev/null
  chmod 000 /dev/.hk_block 2>/dev/null
  mount -o bind /dev/.hk_block /system/xbin/hotacksu 2>/dev/null
fi

# (2) Firmware hard-forces com.htc.magcubicos as HOME at boot. Bring the custom launcher forward.
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done
  sleep 1
  am start -n com.gathin.tvhome/.MainActivity
) &

# (3) Autostart PhairPlay (AirPlay/Miracast/DLNA receiver) after boot, backup to its boot receiver.
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done
  sleep 15
  am start-foreground-service -n com.phairplay.googletv/com.phairplay.service.PhairPlayService 2>/dev/null
) &

# (4) Keep Wi-Fi out of power save (AIC8800 ps_on=Y) so casts do not self-disconnect.
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done
  sleep 12
  cmd wifi force-low-latency-mode enabled 2>/dev/null
  while true; do
    CLASSPATH=/data/adb/modules/hy300tweaks/wifilock.jar app_process32 /system/bin HoldWifiLock 4 >/dev/null 2>&1
    sleep 5
  done
) &

# (5) Re-neutralize BadBox / bloat on every boot. They ship in the read-only stock /product
#     (cannot be deleted) and a firmware update/reset can re-enable them. Disabling for user 0
#     persists in /data and is reversible (cmd package install-existing <pkg>).
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done
  for p in \
    com.android.vending com.htc.storeos com.htc.htcotaupdate \
    com.android.toofifi com.toofifi.lineserver com.htc.eventuploadservice \
    com.softwinner.update android.rockchip.update.service \
    com.htc.samescreen com.mphotool.usbcastserver com.mphotool.airplay \
    com.android.music com.mobisystems.office ; do
    pm uninstall --user 0 "$p" >/dev/null 2>&1
  done
) &
