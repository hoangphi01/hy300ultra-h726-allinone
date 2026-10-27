# HY300 Ultra (Allwinner H726) - Hệ keystone / auto-keystone / autofocus (ghi chép dịch ngược)

> Mục tiêu: mô tả cơ chế keystone đủ chi tiết để sửa tận gốc, và ghi rõ các điểm H726 khác H713. Nguồn: chuỗi `strings` từ `MagcubicOS.apk` và `tpjz_allwinner.apk` (`com.hysd.vafocus`) trên H726, probe trực tiếp thiết bị, và thuật toán vendor đã được dịch ngược sẵn cho H713 trong `allwinner-h713-linux/userspace/h713-keystone/model/*.py` (H726 gần như trùng khớp: cùng `libduRYXtp.so`, cùng tên method `optKeystoneFun` / `getRYXRatiomapDate` / `efect_tp_correct` / `androidN_tp_correct` / `GsInitXYZ` / `checkGsensorDataOk`).
> Ký hiệu trạng thái: "(xác minh)" = đã kiểm trực tiếp trên H726; "(suy luận)" = suy từ H713, cần xác nhận lại bằng jadx/Ghidra.

## 1. Pipeline tổng thể

```
gsensor (MPU-6880)  ->  đọc raw counts (x,y,z)
  |   (kernel: sysfs sensor_xyz_mode / xyz_data, định dạng "%d,%d,%d")
  v
com.hysd.vafocus / LocalService.optKeystoneFun          (suy luận; LocalService.java)
  |   GsX,GsY = atan(counts) - atan(BASELINE)   (trừ đi tư thế gốc do nhà máy đặt)
  v
libduRYXtp.so: getRYXRatiomapDate -> draw_ret_map_point (suy luận; 8 inset góc, đơn vị px panel)
  v
efect_tp_correct -> ghi 8 prop persist.htc.keystone.{ltx,lty,rtx,rty,lbx,lby,rbx,rby} (xác minh)
  |   androidN_tp_correct: đổi thứ tự APP -> PARCEL cho libkeystone
  v
Binder transaction tới SurfaceFlinger (H713: txn 1050)  (suy luận)
  v
SurfaceFlinger (libsurfaceflinger): getKeyStoneMatrix -> K (homography 4x4)
      -> K . projectionMatrix, kết hợp trên GPU (Mali)   (suy luận; dùng GPU, không phải warp phần cứng)
```

Điểm chính: keystone không phải một khối phần cứng và không đi qua co-processor; đây là một phép homography do GPU dựng trong SurfaceFlinger (nhân sau vào projection matrix). Khử răng cưa ở cạnh: `persist.display.keystone.alias.{enable=1,scale=4,edge=20}` (xác minh).

## 2. Thuật toán góc (gsensor -> GsX/GsY) (suy luận, từ `gsensor_angles.py`)

- Góc mỗi trục = `atanf(axis / sqrt(tổng_bình_phương_hai_trục_kia)) * 57.29578` (độ).
- `du = góc(hiện tại) - góc(baseline)`, trong đó GsX là roll quanh trục quang, GsY là pitch (ngửa/cúi).
- Dead-band: khi `|GsX| < 0.3` độ thì quy về 0 (GsY không có dead-band). Khi `counts.z < 0` (máy treo ngược) thì đảo dấu GsX, GsY. `persist.sys.tpwithgsy = 0` thì bỏ qua GsY (chỉ dùng camera).
- Vì công thức dựa trên tỉ lệ (atan của tỉ số counts) nên hệ số counts/g bị triệt tiêu; do đó MPU-6880 (H726) và SC7A20 (H713) khác nhau về counts/g không ảnh hưởng tới góc. Điều kiện cần là một baseline hợp lệ.
- Baseline "phẳng" lý tưởng là `(0, 0, +counts_per_g)` (`LEVEL_REFERENCE`), tương ứng `GsInitXYZ = "0,0,<dương>"`.

## 3. Cơ chế baseline gsensor (xác minh 2026-10-27, jadx MagcubicOS)

> Đính chính: baseline không ghi qua `GsInitXYZ` (giá trị Global/Secure luôn null trên thiết bị này). Luồng vendor trên H726 dùng framework call `PrjScreen.set_angle_offset()`.

- Chụp baseline: `PrjScreen.set_angle_offset()` (một static framework method; MagcubicOS gọi qua reflection, `ReflectUtil.invokeSet_angle_offset()`). Hàm không nhận tham số: nó lấy giá trị gsensor hiện tại làm mốc "phẳng", lưu ở lớp native và ghi một cờ. `get_angle_offset()` đọc lại (trả về số nguyên là góc).
- Kích hoạt bởi `InitAngleActivity` (`com.htc.magcubicos.activity.InitAngleActivity`, trỏ bởi `persist.sys.initActivity`). Màn hình hiển thị: "Please place the device on a horizontal surface and press [OK]... Do not move during correction!". Luồng: `onCreate` gọi `KeystoneUtils_726.resetKeystone()` và tạm tắt auto; bấm OK gọi `set_angle_offset()` (chờ khoảng 3 giây); `finish`; `onStop` bật lại auto nếu trước đó đang bật.
- Nơi lưu bền: file `/oem/gsensor_data` (10 byte, một cờ số nguyên `= 1` nghĩa là "đã calibrate"; `/oem` là partition bền). Giá trị offset góc được giữ ở lớp native/HAL. Tồn tại qua reboot (đã kiểm bằng tắt/mở nguồn: auto vẫn chạy).
- `checkGsensorDataOk` (LocalService:638) yêu cầu: không phải toàn số 0, `z >= 0`, `|x| <= |z|`, `|y| <= |z|`. Do đó khi calibrate máy phải ở tư thế logo xuôi (`z > 0`); tư thế treo ngược (`z < 0`) bị từ chối.
- Trạng thái thiết bị này (xác minh 2026-10-27): sau khi calibrate đúng cách, `/oem/gsensor_data = 1`, `tpryauto = 1`, auto chạy đúng, bám theo góc nghiêng và tồn tại qua reboot. (`GsInitXYZ` vẫn null; H726 không dùng giá trị này cho auto.)

## 4. Hai đường keystone riêng trên H726

> Ở góc nghiêng 15 độ, auto bù hình vuông đúng, nhưng `persist.htc.keystone.*` vẫn bằng 0, cho thấy auto không đi qua `htc.keystone`.

- Đường AUTO (gsensor): `optKeystoneFun` -> `KeystoneUtils_726` -> áp thẳng ở lớp DE/native (homography DRM/KMS). Theo dõi qua `persist.sys.keystoneAngle` / `keystonefinalAngle` (góc đo và góc áp). Không chạm vào `persist.htc.keystone.*`.
- Đường MANUAL (4 góc chỉnh tay): `CorrectionActivity` / `TchhdActivity` -> `persist.htc.keystone.*`.
- Hệ quả: đặt tay `htc.keystone` khi auto đang bật sẽ bị đường auto/DE ghi đè, nên giá trị tay chỉ thỉnh thoảng giữ được. Muốn chỉnh tay thì tắt auto; muốn auto thì để `KeystoneUtils_726` tự quản.

### Prop keystone (xác minh trên H726)

| Prop | Vai trò |
|---|---|
| `persist.htc.keystone.{ltx,lty,rtx,rty,lbx,lby,rbx,rby}` | Đường manual (`CorrectionActivity` / `TchhdActivity`). Auto không dùng. |
| `persist.display.keystone_{ltx..rby}` | Giá trị "đã chấp nhận" (bản clamp đi kèm). Trên thiết bị này còn sót `lbx/lby/rbx/rby = 105/87`, vô hại khi auto chạy. |
| `persist.display.keystone_zoom_h/_v` | Zoom (100 = neutral), qua `libPxScale.so` (MagcubicOS). |
| `persist.sys.tpryauto` | `1` = bật auto-keystone theo gsensor. |
| `persist.sys.keystoneAngle / keystonefinalAngle / keystone_offset` | Góc đo / góc áp / offset. |
| `persist.sys.keystone_vaf / tpryxcrt / uptpryxcrt`, `persist.sys.panelvalue` | Trạng thái vaf / camera-correct; `panelvalue` 0..3 (`optMbKeystoneFun`). |
| Broadcast để áp: `am broadcast -a com.htc.DISPLAY_SETTINGS` và `android.intent.hotack_keystone` | Yêu cầu SurfaceFlinger đọc lại. |

## 5. Quy trình sửa đã xác minh (chạy được và tồn tại qua reboot, 2026-10-27)

> Nguyên nhân gốc của "hình thang ngược / auto sai" là tư thế lắp và chế độ chiếu sai, không phải prop keystone. Thiết bị hình trụ trên gimbal có thể đặt ngang theo hai chiều: logo úp (`z < 0`) đưa firmware vào nhánh "treo ngược" (được kiểm thử ít hơn), ở đó nó đảo dấu bù (`LocalService:1179`, `if z<0: f4=-f4; f5=-f5`) và `checkGsensorDataOk` từ chối baseline, nên auto bù ngược và tạo hình thang ngược. Logo xuôi (`z > 0`) đi vào nhánh "đặt bàn" đã được kiểm thử kỹ.

### 5a. Công tắc lật ảnh (projection mode), không phải prop

- Áp qua `factorySetPanelValue(E_AW_PANEL_CONFIG_MIRROR, mode)` (HAL `AwTvDisplayManager` -> `ITvServer`), đặt bởi `ProjectActivity` (`com.htc.magcubicos`, màn "Chế độ chiếu"). Đọc giá trị hiện tại bằng `factoryGetPanelValue(MIRROR)`.
- Bốn mode: `0` = Front (đặt bàn), `1` = Rear, `2` = Front ceiling (treo trần, lật 180 độ), `3` = Rear ceiling.
- `persist.sys.panelvalue` chỉ là cache UI; `setprop` rồi reboot không lật ảnh. Phải gọi `factorySetPanelValue` (qua menu `ProjectActivity`, bấm trái/phải để chọn mode). Giá trị thật được giữ ở HAL/NV.

### 5b. Các bước sửa (đã chạy đúng)

1. Xoay trụ để logo xuôi và lens nằm ngang (`z > 0`; kiểm bằng `cat /sys/devices/platform/prj343x/xyz_data`, z phải dương, khoảng `+8192`, tức khoảng 1 g). Đây là bước quan trọng nhất.
2. Mở `am start -n com.htc.magcubicos/.activity.ProjectActivity`, chọn mode cho ảnh xuôi (đặt bàn logo xuôi là mode `0`, Front). `factorySetPanelValue` áp ngay.
3. Calibrate baseline: `am start -n com.htc.magcubicos/.activity.InitAngleActivity`, giữ máy ngang và yên, bấm OK; `set_angle_offset()` chụp mốc và ghi `/oem/gsensor_data = 1`.
4. Bật auto: `setprop persist.sys.tpryauto 1` và `am broadcast -a android.intent.hotack_keystone --ei ratio 1 --ei keystone 1`.
5. Kết quả: để ngang thì hình vuông; nghiêng 15 độ thì `keystoneAngle` bám theo và auto bù về vuông; tắt mở nguồn vẫn giữ (baseline nằm ở `/oem`).

Lưu ý: không đặt tay `persist.htc.keystone.*` khi auto đang bật (đường DE của auto sẽ ghi đè, xem mục 4).

### 5c. Tinh chỉnh tay khi auto bật

- Vendor cho phép nhích lên/xuống bằng mũi tên trên remote, chồng lên kết quả auto (lưu ở `persist.sys.keystone_offset`). Giữ nguyên hành vi vendor; điều này đạt được bằng cách chỉ dùng luồng `InitAngleActivity` và `tpryauto`, không khóa cứng tham số nào.

### 5d. Autofocus (tách khỏi keystone)

- Camera UVC và motor bước (`prj_config.ini`: `motor_type`, `motor_step_delay`); `VaFocus = 10` dùng hill-climb theo độ nét.

## 6. Khác biệt giữa H726 và H713

| | H713 (sun50iw12) | H726 (sun50iw15) |
|---|---|---|
| FEL SoC id | `0x1860` | `0x1911` |
| Display co-proc | MIPS `display.bin` (`/dev/mipsloader`), xử lý PQ/gamma/geometry qua MIPS | không có MIPS; DRM/KMS trực tiếp (`/dev/sunxi_drm_heap`, `/dev/sunxi_soc_info`) |
| gsensor | SC7A20 (driver `stk83xx`, 8000 cnt/g, WHO_AM_I `0x11`) | MPU-6880 (`gsensor.cfg: mpu6880_acc`; driver/scale/WHO_AM_I khác) |
| Baseline gsensor | `GsInitXYZ` (Settings.Global/Secure) | khác hẳn: dùng `PrjScreen.set_angle_offset()` (framework), lưu cờ `/oem/gsensor_data`; `GsInitXYZ` = null (không dùng) |
| Áp keystone auto | prop / txn SurfaceFlinger | hai đường tách: auto -> `KeystoneUtils_726` -> DE (không qua `htc.keystone`); manual -> `persist.htc.keystone.*` |
| Lật ảnh (proj mode) | - | `factorySetPanelValue(MIRROR, 0..3)` qua `ProjectActivity`; `persist.sys.panelvalue` chỉ là cache |
| `libkeystone.so` riêng | có (`getKeyStoneMatrix`) | không thấy file riêng (nhiều khả năng gộp trong `libsurfaceflinger.so`) |
| Keystone util | - | class `KeystoneUtils_726` riêng (vendor tách code cho H726) |
| Userspace / libs | arm64 + arm32 | 32-bit armeabi-v7a (`libduRYXtp.so` bản arm) |
| Launcher / settings | luminaos / BigDroid | magcubicos (`MagcubicOS.apk`) |
| Vendor geom lib | `libduRYXtp.so` | `libduRYXtp.so` (giữ nguyên) + `libPxScale.so` (zoom) |
| PQ register path | `/sys/class/sunxi_dump/write` | chưa kiểm (có thể khác do không có MIPS) |

## 7. Việc còn lại để xác nhận đầy đủ (cần chạy công cụ trên máy)

- jadx hai APK để đọc thân các hàm `GsensorUtils.setgsensorInit` / `get_xyz_intVal`, `LocalService.optKeystoneFun`, `VajzActivity` (ghi baseline), `KeystoneUtils_726`, `InitAngleActivity`, xác nhận định dạng và luồng trên H726.
- Ghidra `libduRYXtp.so` (ARM:LE:32) với `getRYXRatiomapDate` / `draw_ret_map_point` / `get_gsensor_du`, so địa chỉ với model H713 để tìm chỗ scale cho MPU-6880.
- Đọc gsensor trực tiếp (sysfs MPU-6880 / IIO `in_accel_*_raw`) để biết máy đang nghiêng hay phẳng.
- Xác nhận SurfaceFlinger có đọc `htc.keystone.*` (thử đặt 92/71 xem có hết méo không).
- Trích `boot-resource.fex` (26 MB) và `Reserve0.fex` (16 MB) để lấy `gamma.tbl` / `LogoRegData.bin` / dữ liệu calibrate panel (phần PQ/màu).
