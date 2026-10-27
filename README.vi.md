<div align="center">

# HY300 Ultra (Gojodoq / Magcubic) - Allwinner H726 - Root, Dịch ngược & Tùy biến

*Thực hiện bởi [hoangphi01](https://github.com/hoangphi01). Dựa trên guide root H726 trên XDA và nền research H713, với [Claude](https://www.anthropic.com/claude) (AI của Anthropic) làm trợ lý nghiên cứu.*

[English](README.md) · **Tiếng Việt**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![SoC](https://img.shields.io/badge/SoC-Allwinner%20H726%20(sun50iw15)-blue)
![FEL ID](https://img.shields.io/badge/FEL%20ID-0x1911-informational)
![Android](https://img.shields.io/badge/Android-14-green)
![Root](https://img.shields.io/badge/root-Magisk-success)
![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)
![Affiliation](https://img.shields.io/badge/affiliation-none-lightgrey)
[![Downloads](https://img.shields.io/badge/Downloads-MEGA-D9272E)](https://mega.nz/folder/5UgxwQiS#EcKh5wCWni6yjOcDvCLXvQ)

<img src="assets/screenshots/home-3col.png" width="680" alt="Màn hình chính tùy biến">

</div>

---

> **Tải về (bản dựng sẵn):** image đã root, firmware stock (để unbrick) và các APK được mirror trên **[MEGA](https://mega.nz/folder/5UgxwQiS#EcKh5wCWni6yjOcDvCLXvQ)** - chỉ cho H726; nhớ verify SHA256 ghi trong folder. Có thể flash thẳng khỏi cần build, xem [02. Rooting](docs/02-rooting.md).

## 1. Tổng quan

Repo này là bản ghi kỹ thuật và kho all-in-one cho máy chiếu **Gojodoq / Magcubic HY300 Ultra**, chạy **Allwinner H726 (`sun50iw15p1`, FEL id `0x1911`)**. HY300 Ultra là máy chiếu Android 14 hình trụ giá rẻ, bán white-label dưới nhiều thương hiệu trên cùng một bo mạch; firmware stock là bản clone bị khóa, đầy app rác quảng cáo/theo dõi và một backdoor họ BadBox.

Công việc trải khắp stack: vào FEL / BootROM, dựng image root minimal-diff xoay quanh **checksum `add_sum` trong V-file** của firmware, dịch ngược bằng jadx/`strings` **subsystem keystone / auto-keystone / autofocus**, bóc và vô hiệu malware **BadBox 2.0**, và dựng lại toàn bộ phần mềm hậu-root.

Repo viết để **bổ sung, không thay thế [guide root H726 trên XDA](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/)**: guide đó lo phần flash; repo này ghi phần nó không có - chi tiết checksum khiến image tùy biến flash được, ruột hệ hiển thị/keystone, bóc malware, và tùy biến. Tất cả đặc thù cho **H726**; máy H713 xem mục [Tương thích](#6-tương-thích).

## 2. Thông số thiết bị

- **SoC:** Allwinner H726 (`sun50iw15p1`), FEL SoC id `0x1911`, FEL USB `1f3a:efe8`
- **CPU:** ARM Cortex-A53 4 nhân
- **GPU:** ARM Mali (chưa xác nhận model trên máy này)
- **RAM:** ~1 GB (thiết bị low-RAM; `ActivityManager.isLowRamDevice = true`)
- **Lưu trữ:** eMMC ~8 GB (`mmcblk0`, dùng được ~7.3 GB)
- **Cảm biến trọng lực:** MPU-6880 (điều khiển auto-keystone; báo z < 0 khi úp ngược)
- **Màn hình:** 720p gốc (`1280x720`), quang học LED
- **Không dây:** Wi-Fi 6 (802.11ax, 5 GHz) + Bluetooth (chưa xác nhận IC)
- **Hệ điều hành:** Android 14 (API 34), build Magcubic `Projector_20250806.111734`
- **Userspace ABI:** 32-bit `armeabi-v7a` (nhiều APK 64-bit / Play-cert sẽ không cài được)
- **Phân vùng:** A/B slot với `init_boot`, `super` động; `verifiedbootstate = orange`, verity tắt; calibrate màu/keystone theo máy nằm ở partition reserve mà flash stock bỏ qua
- **Chuỗi định danh:** `ro.product.model = ADT-3`, alias HY300 Ultra / Pro / Plus / MAX, mã `AC1076`

> Thông số lấy bằng **probe phần mềm + FEL**, không phải tháo máy đo. Linh kiện mức IC (GPU, PMIC, amply, chip Wi-Fi/BT) và phiên bản kernel chính xác **chưa xác nhận** - rất hoan nghênh đóng góp từ ai tháo máy.

## 3. Câu chuyện: từ từng thanh ghi tới bản cài image

Root con này không phải một exploit gọn gàng; nó là cả tuần đâm vào ngõ cụt, và chính các ngõ cụt mới là thứ đáng kể lại.

**Bức tường.** Mọi công cụ công khai cho dòng máy chiếu này - sunxi-fel, U-Boot, các guide root - đều nhắm **H713 (`sun50iw12`, FEL `0x1860`)**. Máy này trả lời FEL bằng `soc=00001911(unknown)`: một SoC *mới hơn*, `sun50iw15`, không BSP, không FEL port, không U-Boot ở đâu cả. Lệnh thường kết nối được rồi... treo.

**Mò xuống mức thanh ghi.** Nên mấy ngày đầu là xử lý từng thanh ghi một. Các script trong [`scripts/`](scripts) quét BootROM tìm địa chỉ reset-vector **RVBAR**, disassemble mask ROM tìm lệnh ghi **RMR** để lật core giữa AArch32 và AArch64, và dò địa chỉ nạp mà SoC mong cho blob first-stage **`fes1`**. Mọi hằng số của H713 đều thử, và cái nào cũng treo ở "unexpected `SCTLR`". Đột phá ở tầng này nhỏ mà rất cụ thể: **`fes1.fex` stock nạp vào SRAM `0x0010C000`**, và U-Boot của hãng có đầu tự chuyển mode ARM32, nên **không** cần nhảy RVBAR ngoài - đúng cái mà luồng H713 ép làm, và là lý do nó treo. Có vậy, FEL mới init được DRAM và chạy bootloader.

**Chướng ngại thật không nằm ở thanh ghi - mà ở bản cài image.** Có đường vào rồi, kế hoạch tưởng đơn giản: vá Magisk vào `init_boot`, flash, xong. PhoenixSuit từ chối mọi image với lỗi phẳng lì *"firmware file is valid"*. Hex-edit thẳng vào `IMAGEWTY` - `init_boot`, `vbmeta`, `dlinfo`, `sys_partition` - chỉ làm tệ hơn. Container giữ **V-file `add_sum` cho từng partition**: một file `V<name>.fex` tí hon chứa checksum `u32` (pad lên bội 4, cộng từng word little-endian). Đổi payload là checksum không khớp nữa, nên bộ flash từ chối cả image. Cách sửa cuối cùng cho ra root là ngừng edit tại chỗ, thay vào đó **thay cả payload rồi tính lại V-file** - copy stock byte-for-byte, chỉ thay `init_boot` (Magisk) và `vbmeta` (tắt verity), rồi ghi lại hai checksum `add_sum`. PhoenixSuit chấp nhận; `su` trả về `uid=0`.

**Có root rồi, công việc quay vào bên trong.** Root là phương tiện, không phải đích. Thói quen decompile-và-dò đó áp vào **hệ hiển thị**: lần theo vì sao auto-keystone cứ méo, đi qua hai đường code keystone riêng và một bug dấu của cảm biến trọng lực, kết lại ở một nhận ra gói gọn một dòng về tư thế máy (mục 5 có bài viết). Cũng sự tò mò đó phát hiện một **backdoor setuid-root** nằm trong `/system/xbin` và cái Play Store giả bên cạnh. Rồi tới phần vui nhất - dựng lại màn hình chính cho ra hồn.

Mỗi mạch đó đều có bài viết riêng ở [Mục lục tài liệu](#5-mục-lục-tài-liệu), giữ nguyên cả các lỗi đã mắc.

## 4. Kết quả & Cột mốc

- [x] **Root** trên H726 / sun50iw15 - `adb shell su` trả về `uid=0(root) ... u:r:magisk:s0`. Theo hiểu biết của tôi là bản root **công khai đầu tiên** của SoC này trên máy chiếu dòng HY300.
- [x] **Đột phá then chốt:** sửa **checksum `add_sum` trong V-file** từng partition để PhoenixSuit chấp nhận image Magisk minimal-diff.
- [x] **Auto-keystone sửa dứt điểm** - gốc rễ là tư thế máy + chế độ chiếu, không phải prop nào; sau một lần calibrate, ảnh tự căn vuông ở mọi độ nghiêng và sống qua reboot.
- [x] **Dịch ngược subsystem keystone / auto-keystone / autofocus** - hai đường keystone riêng, bug dấu trục z của cảm biến, nơi lưu baseline thật (`set_angle_offset` -> `/oem/gsensor_data`), kèm bảng khác biệt H726-vs-H713.
- [x] **Vô hiệu BadBox 2.0** - gỡ `com.android.vending` giả (Tubesky), OTA dropper và telemetry, và che backdoor setuid-root `/system/xbin/hotacksu` bền qua reboot.
- [x] **Tùy biến hậu-root** - màn hình chính (thời tiết, múi giờ thứ hai, âm lịch Việt, screensaver khung tranh, header 3 cột), app "Hub" gộp cài đặt, boot logo, múi giờ.
- [ ] **Còn mở:** AirScreen cho AirPlay; Private DNS chặn quảng cáo; tháo máy lấy thông số IC; port sunxi-fel / U-Boot sạch cho sun50iw15.

## 5. Mục lục tài liệu

### [Phần cứng & root](docs/02-rooting.md)
Định danh bo mạch và đưa Magisk vào:
- **[Định danh phần cứng](docs/01-hardware-id.md):** mã chip, alias máy, cách vào FEL, cách phân biệt H726-vs-H713.
- **[Phương pháp root](docs/02-rooting.md):** FEL + PhoenixSuit + fix checksum `add_sum`.
- **[build_rooted_minimal.py](scripts/build_rooted_minimal.py):** dựng image root minimal-diff từ firmware stock; kèm helper SoC (`find_fes1_addr.py`, `find_rmr.py`, `scan_rvbar.py`).

### [Dịch ngược](docs/03-keystone-display.md)
Ruột hiển thị và keystone của H726:
- **[Subsystem keystone / hiển thị](docs/03-keystone-display.md):** hai đường keystone, bug dấu z của gsensor, nơi lưu baseline, điều khiển chế độ chiếu, và bảng H726-vs-H713.

### [Bảo mật](docs/04-debloat-security.md)
Gỡ malware họ BadBox:
- **[Debloat & gỡ BadBox](docs/04-debloat-security.md):** Play Store / Tubesky giả, OTA dropper, telemetry, và backdoor setuid-root `hotacksu`.
- **[hy300tweaks (module Magisk)](magisk-module/hy300tweaks):** vô hiệu backdoor lúc boot + ép launcher tùy biến làm HOME.
- **[restore-projector.ps1](scripts/restore-projector.ps1):** đảo ngược debloat (cài lại cho user 0).

### [Tùy biến](docs/05-post-root.md)
Dựng lại phần mềm theo ý:
- **[Cấu hình hậu-root](docs/05-post-root.md):** launcher, Hub app, boot logo, múi giờ, app 32-bit nên dùng.
- **[launcher-mods](launcher-mods):** patch màn hình chính trên nền [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
- **[hub-app](hub-app):** `com.hy300.hub`, launcher gộp cài đặt / keystone / casting.

Ghi chú dịch ngược tiếng Việt ở [`docs/vi/`](docs/vi).

## 6. Tương thích

**Có phải máy của bạn?**

| | Repo này (**H726**) | Không phải (**H713**) |
|---|---|---|
| SoC | `sun50iw15p1` (H726) | `sun50iw12p1` (H713) |
| FEL SoC id | `0x1911` | `0x1860` |
| Đường hiển thị | DRM/KMS thẳng (không MIPS) | MIPS co-proc (`display.bin`) |
| Cảm biến trọng lực | MPU-6880 | SC7A20 |

**Kiểm tra máy bạn:** `adb shell getprop ro.hardware` phải ra `sun50iw15p1`; trong chế độ FEL `sunxi-fel version` phải ra `soc=00001911`. Chi tiết: [docs/01](docs/01-hardware-id.md).

Nếu thấy `sun50iw12` / `0x1860` thì đó là **H713** - hãy dùng các dự án mà repo này dựa trên: [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), và [thread H713 trên XDA](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/).

## 7. Miễn trừ & trách nhiệm

- **Trách nhiệm người dùng.** Flash thiết bị Allwinner có thể **brick** vĩnh viễn. Bạn tự chịu trách nhiệm với phần cứng và tuân thủ luật địa phương.
- **Không bảo hành.** Tài liệu cung cấp "nguyên trạng", không bảo hành hay hỗ trợ dưới bất kỳ hình thức nào.
- **Chỉ H726.** Quy trình này nhắm H726 (`sun50iw15` / `0x1911`). **Đừng** áp dụng cho H713 hay SoC khác (xem mục 6).
- **Cảnh báo malware.** Firmware stock là bản clone white-label thuộc họ **BigDroidOS / Bigpanzi** và bị gắn cờ **BadBox 2.0**. Repo chỉ cách phát hiện + vô hiệu, nhưng firmware clone không bao giờ tin tuyệt đối: **đừng** đăng nhập tài khoản cá nhân, ngân hàng hay Google, và cô lập mạng cho máy.
- **Không redistribute.** Repo không kèm firmware, APK hay thư viện proprietary; chỉ gồm phần việc gốc. Firmware chỉ link tới nguồn, không host.

## 8. Lời cảm ơn & Tham khảo

Công trình này dựa trên, và biết ơn:

- **guide root H726 trên XDA** - [*Rooting tutorial for a Magcubic HY300 Pro (Allwinner H726, Android 14)*](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/);
- **nền research H713** - [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), bản sunxi-tools / Linux + model keystone của well0nez, [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root), [schaumann-byte/hy300-projector-homeassistant](https://github.com/schaumann-byte/hy300-projector-homeassistant);
- **tham chiếu gỡ BadBox** - [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal);
- và **[Claude](https://www.anthropic.com/claude) (AI của Anthropic), dùng như trợ lý nghiên cứu** xuyên suốt. Claude hỗ trợ như một công cụ và **không** là git co-author; chủ repo ra quyết định, chịu rủi ro và chạy phần cứng.

**Guides** - [Root H726 (XDA)](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/) · [Root H713 (XDA)](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/)
**Công cụ** - [Magisk](https://github.com/topjohnwu/Magisk) · [sunxi-tools](https://github.com/linux-sunxi/sunxi-tools) · [OpenixSuit](https://github.com/YuzukiTsuru/OpenixSuit) · [imagewty-tool](https://github.com/uictorius/imagewty-tool) · [MagiskPatcher](https://circlecashteam.github.io/MagiskPatcher/) · [jadx](https://github.com/skylot/jadx) (Apache-2.0) · PhoenixSuit (Allwinner)
**Phần mềm hậu-root** - [launcher cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT) · [SmartTube](https://github.com/yuliskov/SmartTube) · [Material Files](https://github.com/zhanghai/MaterialFiles) · Aurora Store · [Lucide icons](https://github.com/lucide-icons/lucide) · [Open-Meteo](https://open-meteo.com) · Outfit font (OFL)
**Firmware** (chỉ link, không redistribute) - androidpctv.com · magcubic-support.com. Ảnh nền của theme launcher thuộc CC-BY / CC-BY-SA / CC0 của tác giả (xem `CREDITS.md` ở repo gốc).

## 9. Giấy phép

[MIT](LICENSE) cho phần việc gốc trong repo. **Không** bao gồm các dự án upstream (mỗi dự án giữ license riêng) hay firmware/APK/thư viện proprietary - những thứ đó **không** có trong repo. Xem ghi chú phạm vi trong [LICENSE](LICENSE).

---

*Không liên kết với Allwinner, Magcubic, Gojodoq, Google hay Anthropic.*
