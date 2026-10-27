# Restore the packages debloated from the HY300 Ultra projector (reversible — reinstalls for user 0).
# Khôi phục các app đã gỡ trên máy chiếu HY300 Ultra (cài lại cho user 0, không mất dữ liệu).
#
# Run / Chạy:  powershell -ExecutionPolicy Bypass -File restore-projector.ps1
# Needs / Cần: Wireless debugging ON on the projector and the PC already paired.
#
# >>> Set your projector's IP here / Điền IP máy chiếu của bạn vào đây <<<
$PROJECTOR_IP = "192.168.1.100"      # e.g. find it in the router or Settings > Network

$adb = "$env:LOCALAPPDATA\platform-tools-dl\platform-tools\adb.exe"
$S   = "${PROJECTOR_IP}:5555"

& $adb connect $S | Out-Null

# Packages removed on 2026-10-25 (adjust to your own debloat list)
$pkgs = @(
  'com.android.toofifi',            # HanYing H713 adware (suspected Bigpanzi backdoor)
  'com.toofifi.lineserver',         # USBCastServer, same toofifi namespace (suspected)
  'com.hysd.vafocus',               # keystone/autofocus (re-enable FIRST if focus/keystone breaks)
  'com.htc.eventuploadservice',     # telemetry
  'com.htc.storeos',                # vendor app store
  'com.htc.htcotaupdate',           # OTA
  'com.softwinner.update',          # Allwinner OTA
  'android.rockchip.update.service',# OTA
  'com.mobisystems.office',         # Office
  'com.android.music',              # Music
  'com.softwinner.TvdVideo',        # video player
  'com.softwinner.awlivetv'         # Live TV
)

foreach ($p in $pkgs) {
  $r = & $adb -s $S shell "cmd package install-existing $p 2>&1"
  "{0,-42} => {1}" -f $p, ($r -join ' ')
}
"Done. Reboot the projector to be safe. / Xong. Khởi động lại máy chiếu cho chắc."
