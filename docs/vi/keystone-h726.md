# HY300 Ultra (Allwinner H726) — Keystone / Auto-Keystone / Autofocus subsystem (reverse-engineering)

> Mục tiêu: hiểu TẬN GỐC cơ chế keystone để sửa dứt điểm (không "băng bó"), và ghi rõ **H726 khác H713 chỗ nào** (cho bài XDA). Nguồn: strings từ `MagcubicOS.apk`+`tpjz_allwinner.apk` (= com.hysd.vafocus) trên H726, probe thiết bị H726, và thuật toán vendor **đã được RE sẵn cho H713** trong `allwinner-h713-linux/userspace/h713-keystone/model/*.py` (H726 sao y gần như 1-1: cùng `libduRYXtp.so`, cùng tên method `optKeystoneFun/getRYXRatiomapDate/efect_tp_correct/androidN_tp_correct/GsInitXYZ/checkGsensorDataOk`).
> **Đánh dấu:** ✅=xác minh trên H726, 🔸=suy từ H713 (cần xác nhận bằng jadx/Ghidra).

## 1. Pipeline tổng thể
```
gsensor (MPU-6880)  →  đọc raw counts (x,y,z)
  │   (kernel: sysfs sensor_xyz_mode / xyz_data in "%d,%d,%d")
  ▼
com.hysd.vafocus / LocalService.optKeystoneFun         🔸 (LocalService.java)
  │   GsX,GsY = atan(counts) − atan(BASELINE)   ← trừ tư thế gốc (factory)
  ▼
libduRYXtp.so: getRYXRatiomapDate → draw_ret_map_point  🔸 (→ 8 inset góc, đơn vị px panel)
  │
  ▼
efect_tp_correct → ghi 8 prop persist.htc.keystone.{ltx,lty,rtx,rty,lbx,lby,rbx,rby}  ✅ PROP LIVE
  │   androidN_tp_correct: đổi thứ tự APP→PARCEL cho libkeystone
  ▼
Binder transaction tới SurfaceFlinger (H713: txn 1050)   🔸
  │
  ▼
SurfaceFlinger (libsurfaceflinger): getKeyStoneMatrix → K (homography 4×4)
      → K · projectionMatrix, vẽ trên GPU (Mali)          🔸 (GPU, KHÔNG phải warp phần cứng)
```
**Điểm mấu chốt:** keystone **không** phải block phần cứng, **không** qua co-processor — nó là **homography do GPU vẽ trong SurfaceFlinger** (post-multiply vào projection matrix). Anti-alias cạnh: `persist.display.keystone.alias.{enable=1,scale=4,edge=20}` ✅.

## 2. Thuật toán góc (gsensor → GsX/GsY)  🔸 từ `gsensor_angles.py`
- Góc mỗi trục = `atanf(axis / sqrt(sum_sq_hai_trục_kia)) × 57.29578` (độ).
- **`du = góc(hiện tại) − góc(BASELINE)`** → `GsX` = roll quanh trục quang, `GsY` = pitch (ngửa/cúi).
- Dead-band: |GsX|<**0.3°** → 0 (GsY KHÔNG có dead-band). `counts.z<0` (máy treo ngược) → đảo dấu GsX,GsY. `persist.sys.tpwithgsy=0` → bỏ qua GsY (chỉ dùng camera).
- **Quan trọng:** vì là **tỉ lệ** (atan của tỉ số counts), **hệ số counts/g bị triệt tiêu** → MPU-6880 (H726) khác SC7A20 (H713) về counts/g **KHÔNG ảnh hưởng góc**. Chỉ cần baseline hợp lệ.
- **Baseline "phẳng" lý tưởng = `(0, 0, +counts_per_g)`** (`LEVEL_REFERENCE`). Tức `GsInitXYZ="0,0,<dương>"`.

## 3. BASELINE gsensor — cơ chế THẬT (✅ xác minh 2026-10-27, jadx MagcubicOS)
> **ĐÃ SỬA LẠI:** baseline KHÔNG ghi qua `GsInitXYZ` (Global/Secure luôn `null` trên unit này). Luồng vendor H726 dùng **framework call** `PrjScreen.set_angle_offset()`.
- **Capture baseline = `PrjScreen.set_angle_offset()`** (static framework method; MagcubicOS gọi qua reflection — `ReflectUtil.invokeSet_angle_offset()`). Không tham số → **chụp gsensor hiện tại làm mốc "phẳng"** rồi lưu native + ghi cờ. `get_angle_offset()` đọc lại (trả int = góc).
- **Kích hoạt bởi `InitAngleActivity`** (`com.htc.magcubicos.activity.InitAngleActivity`, = `persist.sys.initActivity`). Màn hiện: *"Please place the device on a horizontal surface and press [OK]… Do not move during correction!"*. Luồng: onCreate `KeystoneUtils_726.resetKeystone()` + tắt auto tạm → bấm OK `set_angle_offset()` (chờ 3s) → finish → onStop bật lại auto nếu trước đó bật.
- **Nơi lưu bền:** file **`/oem/gsensor_data`** (10 byte, int cờ `=1` "đã calibrate"; /oem = partition bền). Giá trị offset góc giữ trong native/HAL. **Sống qua reboot ✅** (đã test tắt-mở: auto vẫn chạy).
- `checkGsensorDataOk` (LocalService:638): *không all-zero, **z≥0**, |x|≤|z|, |y|≤|z|* → **bắt buộc máy ở tư thế logo XUÔI (z>0)** lúc calibrate; z<0 (treo ngược) bị từ chối.
- **TRẠNG THÁI UNIT NÀY (✅ 2026-10-27):** sau khi calibrate đúng cách → `/oem/gsensor_data=1`, `tpryauto=1`, auto **chạy đúng, bám góc nghiêng, sống qua reboot**. (`GsInitXYZ` vẫn null — H726 KHÔNG dùng nó cho auto.)

## 4. HAI đường keystone riêng trên H726 (🔑 phát hiện lớn — giải thích "lúc được lúc không")
> Ở góc nghiêng 15° auto bù hình VUÔNG đúng, **nhưng `persist.htc.keystone.*` vẫn = 0**. → Auto KHÔNG đi qua htc.keystone.
- **Đường AUTO (gsensor):** `optKeystoneFun` → `KeystoneUtils_726` → áp thẳng **DE/native** (DRM/KMS homography). Theo dõi qua `persist.sys.keystoneAngle` / `keystonefinalAngle` (góc đo/áp). **Không đụng** `persist.htc.keystone.*`.
- **Đường MANUAL (4 góc tay):** `CorrectionActivity` / `TchhdActivity` → `persist.htc.keystone.*`.
- ⇒ Set tay `htc.keystone` khi **auto đang bật** sẽ bị đường auto/DE ghi đè → "lúc được lúc không". Muốn tay thì tắt auto; muốn auto thì để KeystoneUtils_726 tự quản.

### Prop keystone (✅ trên H726)
| Prop | Vai trò |
|---|---|
| `persist.htc.keystone.{ltx,lty,rtx,rty,lbx,lby,rbx,rby}` | Đường **MANUAL** (CorrectionActivity/TchhdActivity). Auto KHÔNG dùng. |
| `persist.display.keystone_{ltx..rby}` | Giá trị "đã chấp nhận" (clamp partner). ⚠ unit này còn rác `lbx/lby/rbx/rby=105/87` sót — vô hại khi auto chạy. |
| `persist.display.keystone_zoom_h/_v` | Zoom (100=neutral), qua `libPxScale.so` (MagcubicOS). |
| `persist.sys.tpryauto` | 1=bật auto-keystone gsensor. |
| `persist.sys.keystoneAngle / keystonefinalAngle / keystone_offset` | Góc đo / góc áp / offset. |
| `persist.sys.keystone_vaf / tpryxcrt / uptpryxcrt`, `persist.sys.panelvalue` | Trạng thái vaf/camera-correct; panelvalue 0..3 (optMbKeystoneFun). |
| Broadcast áp: `am broadcast -a com.htc.DISPLAY_SETTINGS` và `android.intent.hotack_keystone` | Kích SurfaceFlinger đọc lại. |

## 5. ⭐ FIX DỨT ĐIỂM — đã xác minh chạy + sống qua reboot (2026-10-27)
> Gốc rễ "hình thang ngược / auto sai" = **tư thế + chế độ chiếu sai**, KHÔNG phải prop keystone. Máy hình trụ trên gimbal có thể bẻ ngang theo 2 chiều: **logo úp (z<0)** → firmware vào nhánh "treo ngược" (ít test): đảo dấu bù (LocalService:1179 `if z<0: f4=-f4;f5=-f5`) + `checkGsensorDataOk` từ chối baseline → auto bù ngược = hình thang ngược. **Logo xuôi (z>0)** → nhánh "đặt bàn" đã test kỹ.

### 5a. Công tắc LẬT ẢNH (projection mode) — KHÔNG phải prop
- Áp qua **`factorySetPanelValue(E_AW_PANEL_CONFIG_MIRROR, mode)`** (HAL `AwTvDisplayManager`→`ITvServer`), set bởi **`ProjectActivity`** (`com.htc.magcubicos`, màn "Chế độ chiếu"). Đọc hiện tại = `factoryGetPanelValue(MIRROR)`.
- 4 mode: **0=Front (đặt bàn), 1=Rear, 2=Front ceiling (treo trần, lật 180°), 3=Rear ceiling.**
- ⚠ **`persist.sys.panelvalue` chỉ là CACHE UI** — `setprop` + reboot **KHÔNG** lật ảnh. Phải gọi `factorySetPanelValue` (qua menu ProjectActivity, bấm trái/phải chọn mode). Giá trị thật giữ trong HAL/NV.

### 5b. Công thức sửa (đã chạy OK)
1. **Bẻ trụ để LOGO XUÔI + lens ngang** (z>0 — kiểm `cat /sys/devices/platform/prj343x/xyz_data`, z phải DƯƠNG ~+8400). Đây là bước quan trọng nhất.
2. **Mở `am start -n com.htc.magcubicos/.activity.ProjectActivity`** → chọn mode cho ảnh XUÔI (máy để bàn logo-xuôi = mode 0 Front). (factorySetPanelValue áp live.)
3. **Calibrate baseline:** `am start -n com.htc.magcubicos/.activity.InitAngleActivity` → giữ máy ngang yên → bấm **OK** → `set_angle_offset()` chụp mốc → `/oem/gsensor_data=1`.
4. **Bật auto:** `setprop persist.sys.tpryauto 1` + `am broadcast -a android.intent.hotack_keystone --ei ratio 1 --ei keystone 1`.
5. ✅ Kết quả: ngang=vuông; nghiêng 15°→`keystoneAngle` bám theo, auto bù về vuông; **tắt-mở lại vẫn chạy** (baseline ở /oem).
- **KHÔNG** set tay `persist.htc.keystone.*` khi auto bật (đường DE auto ghi đè — xem §4).

### 5c. UX nudge tay khi auto bật (yêu cầu user)
- Vendor cho tinh chỉnh LÊN/XUỐNG bằng mũi tên remote chồng lên auto (lưu `persist.sys.keystone_offset`). Để nguyên hành vi vendor — đã đạt bằng cách chỉ dùng luồng InitAngleActivity + tpryauto, không khoá cứng gì.

### 5d. Autofocus (tách khỏi keystone)
- Camera UVC + motor bước (`prj_config.ini: motor_type, motor_step_delay`); `VaFocus=10` hill-climb độ nét.

## 6. H726 ⚠️ KHÁC H713 (cho bài XDA)
| | H713 (sun50iw12) | **H726 (sun50iw15)** |
|---|---|---|
| FEL SoC id | 0x1860 | **0x1911** |
| **Display co-proc** | **MIPS `display.bin`** (`/dev/mipsloader`) — PQ/gamma/geometry qua MIPS | **KHÔNG có MIPS** ✅ → **DRM/KMS** thẳng (`/dev/sunxi_drm_heap`, `/dev/sunxi_soc_info`) |
| **gsensor** | SC7A20 (driver `stk83xx`, 8000 cnt/g, WHO_AM_I 0x11) | **MPU-6880** ✅ (`gsensor.cfg: mpu6880_acc`; driver/scale/WHO_AM_I khác) |
| **Baseline gsensor** | `GsInitXYZ` (Settings.Global/Secure) | **KHÁC HẲN** ✅: dùng `PrjScreen.set_angle_offset()` (framework), lưu cờ **`/oem/gsensor_data`**; `GsInitXYZ` = null (không dùng) |
| **Áp keystone auto** | prop/txn SurfaceFlinger | **2 đường tách:** auto→`KeystoneUtils_726`→DE (không qua htc.keystone); manual→`persist.htc.keystone.*` ✅ |
| **Lật ảnh (proj mode)** | — | `factorySetPanelValue(MIRROR, 0..3)` qua `ProjectActivity`; `persist.sys.panelvalue` chỉ là cache ✅ |
| `libkeystone.so` riêng | có (`getKeyStoneMatrix`) | **không thấy file riêng** ✅ (chắc gộp trong `libsurfaceflinger.so`) |
| Keystone util | — | class **`KeystoneUtils_726`** riêng ✅ (vendor tách code cho H726) |
| Userspace/libs | arm64 + arm32 | **32-bit armeabi-v7a** ✅ (`libduRYXtp.so` arm) |
| Launcher/settings | luminaos/BigDroid | **magcubicos** (`MagcubicOS.apk`) ✅ |
| Vendor geom lib | `libduRYXtp.so` | **`libduRYXtp.so`** (giữ) + `libPxScale.so` (zoom) ✅ |
| PQ register path | `/sys/class/sunxi_dump/write` | (chưa kiểm — có thể khác do không MIPS) |

## 7. Việc còn lại để chắc 100% (cần chạy tool / máy online)
- [ ] jadx 2 APK → đọc thân `GsensorUtils.setgsensorInit/get_xyz_intVal`, `LocalService.optKeystoneFun`, `VajzActivity` (ghi baseline), `KeystoneUtils_726`, `InitAngleActivity` → xác nhận định dạng + luồng H726. *(classifier chặn mình chạy jadx → user chạy, mình đọc output.)*
- [ ] Ghidra `libduRYXtp.so` (ARM:LE:32) → `getRYXRatiomapDate/draw_ret_map_point/get_gsensor_du` — diff địa chỉ với model H713, bắt chỗ scale MPU-6880.
- [ ] Đọc **gsensor live** (sysfs MPU-6880 / IIO `in_accel_*_raw`) → biết máy đang nghiêng hay phẳng → quyết auto vs tay.
- [ ] Xác nhận SurfaceFlinger đọc `htc.keystone.*` (test set 92/71 → hết méo?).
- [ ] Trích `boot-resource.fex`(26MB)/`Reserve0.fex`(16MB) → `gamma.tbl`/`LogoRegData.bin`/panel calibration (phần PQ/màu).
