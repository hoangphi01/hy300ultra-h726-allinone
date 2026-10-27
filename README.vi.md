<div align="center">

# HY300 Ultra — Allwinner H726 — All-in-One

**Root · dịch ngược · debloat/bảo mật · tùy biến hậu-root**
cho máy chiếu **Gojodoq / Magcubic HY300 Ultra** (Allwinner **H726 / sun50iw15**).

[🇬🇧 English](README.md) · 🇻🇳 **Tiếng Việt**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![SoC](https://img.shields.io/badge/SoC-Allwinner%20H726%20(sun50iw15)-blue)
![FEL ID](https://img.shields.io/badge/FEL%20ID-0x1911-informational)
![Android](https://img.shields.io/badge/Android-14-green)
![Root](https://img.shields.io/badge/root-Magisk-success)
![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)
![Affiliation](https://img.shields.io/badge/affiliation-none-lightgrey)

<img src="assets/screenshots/home-3col.png" width="640" alt="Màn hình chính tùy biến">

</div>

---

## ⚠️ Miễn trừ — đọc trước

- **Tự chịu rủi ro.** Flash thiết bị Allwinner có thể **brick** (chết máy). Bạn tự chịu trách nhiệm với phần cứng của mình. Không bảo hành.
- **Chỉ dành cho ĐÚNG 1 con chip: Allwinner `H726` = `sun50iw15` = FEL SoC id `0x1911`.** Cùng bo mạch này bán dưới các tên **Gojodoq / Magcubic HY300 Ultra / Pro / Plus / MAX**, model firmware **`ADT-3`**, mã **`AC1076`**.
- **Nếu máy bạn là `H713` (`sun50iw12`, FEL id `0x1860`) — repo này KHÔNG dành cho bạn.** Hãy sang các dự án H713 mà repo này dựa trên: [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), và [thread H713 trên XDA](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/). Xem [cách kiểm tra chip](docs/01-hardware-id.md).
- **Firmware gốc trên các máy này là bản clone white-label** thuộc họ BigDroidOS / Bigpanzi và bị gắn cờ **BadBox 2.0** (residential-proxy / telemetry / OTA tự cài lại, kèm **backdoor setuid-root**). Repo này ghi lại cách phát hiện + vô hiệu, nhưng firmware clone thì không bao giờ tin tuyệt đối được — **đừng đăng nhập tài khoản cá nhân / ngân hàng / Google; hãy cô lập mạng cho máy.**
- **Không redistribute firmware, APK, hay thư viện `.so` của hãng.** Chỉ gồm phần việc gốc (script, docs, bản vá launcher, app Hub); phần còn lại chỉ link tới nguồn.

---

## 🙏 Lời cảm ơn

Repo này đứng trên vai người khác. Dựa trên:

- **guide root H726 trên XDA** — [*Rooting tutorial for a Magcubic HY300 Pro (Allwinner H726, Android 14)*](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/),
- **nền tảng research H713** — [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), bản sunxi-tools/Linux + model keystone của well0nez, [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root),
- **tham chiếu dọn BadBox** — [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal),
- và với **[Claude](https://www.anthropic.com/claude) (AI của Anthropic) làm trợ lý nghiên cứu**,

…tôi đã phát triển và ghi lại những phần mà các guide sẵn có chưa đề cập:

1. **Root H726 / sun50iw15** nhờ sửa **checksum `add_sum` trong V-file** của từng partition để PhoenixSuit chấp nhận image Magisk minimal-diff (cách hex-edit mà ai cũng thử đầu tiên sẽ **phá** checksum IMAGEWTY). → [`docs/02-rooting.md`](docs/02-rooting.md)
2. **Dịch ngược đầy đủ subsystem keystone / auto-keystone / autofocus trên H726** — *hai* đường keystone riêng, **bug dấu trục z** của gsensor làm auto bù ngược, và cơ chế baseline thật (`PrjScreen.set_angle_offset()` → `/oem/gsensor_data`). Kèm **bảng khác biệt H726 vs H713**. → [`docs/03-keystone-display.md`](docs/03-keystone-display.md)
3. **Gỡ BadBox 2.0**, gồm backdoor setuid-root `/system/xbin/hotacksu` và `com.android.vending` giả (Tubesky). → [`docs/04-debloat-security.md`](docs/04-debloat-security.md)
4. Bộ **tùy biến hậu-root** hoàn chỉnh (launcher có thời tiết / đồng hồ kép / âm lịch / screensaver khung tranh, app "Hub" gộp cài đặt, boot logo, múi giờ). → [`docs/05-post-root.md`](docs/05-post-root.md)

> Claude hỗ trợ như một công cụ; **không** được ghi là git co-author. Con người (chủ repo) ra quyết định, chịu rủi ro, và chạy phần cứng.

---

## 📟 Có phải máy của bạn?

| | ✅ Repo này (**H726**) | ❌ Không phải (**H713**) |
|---|---|---|
| SoC | `sun50iw15p1` (H726) | `sun50iw12` (H713) |
| FEL SoC id | **`0x1911`** | `0x1860` |
| Đường hiển thị | DRM/KMS thẳng (không MIPS) | MIPS co-proc (`display.bin`) |
| gsensor | MPU-6880 | SC7A20 |
| Đi đâu | **ở đây** | xem phần Miễn trừ |

**Kiểm tra máy bạn:** `adb shell getprop ro.hardware` (phải ra `sun50iw15p1`), hoặc trong chế độ FEL `sunxi-fel version` (phải ra `soc=00001911`). Chi tiết ở [`docs/01-hardware-id.md`](docs/01-hardware-id.md).

---

## 🧭 Các bước

1. **[Định danh phần cứng](docs/01-hardware-id.md)** — xác nhận chip, các tên máy, cách vào FEL.
2. **[Root](docs/02-rooting.md)** — FEL + PhoenixSuit + fix `add_sum`; build image bằng [`scripts/build_rooted_minimal.py`](scripts/build_rooted_minimal.py).
3. **[Debloat & bảo mật](docs/04-debloat-security.md)** — gỡ rác, vô hiệu backdoor BadBox (xem [`magisk-module/hy300tweaks`](magisk-module/hy300tweaks)).
4. **[Keystone & hiển thị](docs/03-keystone-display.md)** — sửa auto-keystone dứt điểm (tư thế + chế độ chiếu + calibrate).
5. **[Tùy biến hậu-root](docs/05-post-root.md)** — launcher tùy biến ([`launcher-mods/`](launcher-mods)) và app Hub ([`hub-app/`](hub-app)).

> **Image đã root (~2.2 GB) KHÔNG host ở đây** — GitHub không chứa nổi, và firmware gốc là proprietary. Script build sẽ tái tạo image từ firmware stock.

---

## 🧩 Hành trình — những lỗi đã dạy ra cách làm

Việc thật thì lộn xộn. Đây là các ngõ cụt, phòng khi giúp bạn tiết kiệm thời gian:

- **Hex-edit thẳng vào IMAGEWTY → PhoenixSuit từ chối.** Container có file checksum `V<name>.fex` cho từng partition (`add_sum`: pad lên bội 4, cộng các `u32` LE). Đổi payload là phải tính lại V-file, không thì bị reject. *Đây chính là chìa khóa để root.*
- **Set nhầm prop keystone.** `persist.display.keystone_*` là bản "đã chấp nhận"; đường manual live là `persist.htc.keystone.*`; còn **auto-keystone không dùng cái nào** — nó áp qua `KeystoneUtils_726` → display engine. Hai đường riêng → nên set tay "lúc được lúc không".
- **"Hình thang ngược".** Gốc rễ không phải prop — là do **đặt trụ máy logo-úp (gsensor z < 0)**, đẩy firmware vào nhánh "treo trần" (đảo chiều bù + từ chối lưu baseline). Lật logo-xuôi (z > 0) là chạy.
- **`persist.sys.panelvalue` chỉ là cache UI** — `setprop` + reboot *không* lật ảnh; phải gọi `factorySetPanelValue` qua màn Chế độ chiếu.
- **Magic-mount của Magisk hỏng trên firmware này** — overlay `system/` không áp; dùng `/oem`, ghi thẳng partition, hoặc `service.d`.

Chi tiết + cách sửa ở [`docs/`](docs).

---

## 📂 Cấu trúc repo

```
docs/            Bài viết kỹ thuật (tiếng Anh; bản gốc tiếng Việt ở docs/vi/)
scripts/         Script build image root + helper phân tích SoC (Python) + restore (PowerShell)
launcher-mods/   Patch + file cho màn hình chính tùy biến (trên nền Gathin23/cleanyourtv)
hub-app/         Mã app "Hub" (com.hy300.hub) — gộp cài đặt/keystone/casting
magisk-module/   hy300tweaks — vô hiệu backdoor hotacksu + ép launcher làm HOME
assets/          Boot logo + ảnh chụp màn
```

---

## 📚 Tham khảo & trích dẫn

**Guides**
- Root H726 (XDA): https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/
- Root H713 (XDA, nền tảng): https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/

**Research H713 (nền tảng kỹ thuật)**
- https://github.com/probonopd/hy310-deshittify
- https://github.com/YuujiLab/HY300-H713-Research
- https://github.com/gniax/magcubic-hy300-root
- https://github.com/schaumann-byte/hy300-projector-homeassistant
- well0nez — sunxi-tools (fork H713) + `allwinner-h713-linux` (model RE keystone dùng làm tham chiếu 1:1)

**Bảo mật**
- https://github.com/maximilian-sh/hy350max-badbox-removal

**Công cụ**
- Magisk — https://github.com/topjohnwu/Magisk
- sunxi-tools — https://github.com/linux-sunxi/sunxi-tools
- OpenixSuit — https://github.com/YuzukiTsuru/OpenixSuit
- imagewty-tool — https://github.com/uictorius/imagewty-tool
- MagiskPatcher (online) — https://circlecashteam.github.io/MagiskPatcher/
- jadx — https://github.com/skylot/jadx *(Apache-2.0)*
- PhoenixSuit — công cụ flash của Allwinner

**Phần mềm hậu-root**
- Launcher: https://github.com/Gathin23/cleanyourtv *(MIT — màn hình chính ở đây là bản vá trên nền này)*
- SmartTube — https://github.com/yuliskov/SmartTube
- Material Files — https://github.com/zhanghai/MaterialFiles
- Aurora Store · Lucide icons (https://github.com/lucide-icons/lucide) · Open-Meteo API (https://open-meteo.com) · Outfit font *(OFL)*

**Firmware** *(chỉ link, không redistribute)* — androidpctv.com · magcubic-support.com

Ảnh nền do theme launcher kèm theo thuộc **CC-BY / CC-BY-SA / CC0** của tác giả; xem `CREDITS.md` trong repo launcher gốc.

---

## 📄 Giấy phép

[MIT](LICENSE) cho phần việc gốc trong repo. **Không** bao gồm các dự án upstream (mỗi dự án giữ license riêng) hay firmware/APK/thư viện proprietary của hãng — những thứ đó **không** có trong repo. Xem ghi chú phạm vi trong [LICENSE](LICENSE).

*Không liên kết với Allwinner, Magcubic, Gojodoq, Google, hay Anthropic.*
