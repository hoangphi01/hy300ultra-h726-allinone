#!/system/bin/sh
# (1) Neutralize the BadBox setuid-root backdoor /system/xbin/hotacksu: /system is
#     read-only so it can't be deleted in place; mask it with an empty, non-executable
#     file. The bind (done as root in the global mount namespace) propagates to all
#     processes, so the backdoor can no longer be exec'd to gain root.
if [ -u /system/xbin/hotacksu ]; then
  printf '' > /dev/.hk_block 2>/dev/null
  chmod 000 /dev/.hk_block 2>/dev/null
  mount -o bind /dev/.hk_block /system/xbin/hotacksu 2>/dev/null
fi

# (2) Firmware hard-forces com.htc.magcubicos as HOME at boot (framework patch: ignores
#     preferred activity, HOME role, persist.sys.homePackage and pm disable). Bring the
#     custom launcher forward once, gently, after boot completes.
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
  done
  sleep 1
  am start -n com.gathin.tvhome/.MainActivity
) &

# (3) Optional: autostart a screen-mirroring receiver after boot (here PhairPlay, a
#     free, ad-free AirPlay / Miracast / DLNA receiver used in place of AirScreen).
#     The app has its own BOOT_COMPLETED receiver, but the firmware task-killer (awbms)
#     can drop it, so start it here as a backup. Service only, so no UI covers the home
#     screen. Remove this block, or change the component, if you do not use PhairPlay.
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
  done
  # wait for Wi-Fi to come up (the receiver needs the network to advertise)
  sleep 15
  am start-foreground-service -n com.phairplay.googletv/com.phairplay.service.PhairPlayService 2>/dev/null
) &

# (4) Keep Wi-Fi out of power save while casting. The AIC8800 chip has ps_on=Y (power save
#     on, and the parameter is read-only at runtime) and the receiver holds only a multicast
#     lock, so the stream drops intermittently (self-disconnect). Force low-latency mode and
#     hold a FULL_LOW_LATENCY Wi-Fi lock from a tiny root daemon (scripts/HoldWifiLock.java,
#     compiled to wifilock.jar with javac -> d8 and placed in this module dir); respawn it if
#     it exits so the lock is always held. Harmless on a mains-powered projector.
(
  while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
  done
  sleep 12
  cmd wifi force-low-latency-mode enabled 2>/dev/null
  while true; do
    CLASSPATH=/data/adb/modules/hy300tweaks/wifilock.jar app_process32 /system/bin HoldWifiLock 4 >/dev/null 2>&1
    sleep 5
  done
) &
