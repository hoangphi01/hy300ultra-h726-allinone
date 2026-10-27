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
