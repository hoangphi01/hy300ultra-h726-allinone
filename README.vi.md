<div align="center">

# HY300 Ultra — Allwinner H726 — All-in-One

Root, dịch ngược, gỡ malware và tùy biến máy chiếu
**Gojodoq / Magcubic HY300 Ultra** — Allwinner **H726 (sun50iw15 / FEL 0x1911)**.

[English](README.md) · **Tiếng Việt**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![SoC](https://img.shields.io/badge/SoC-Allwinner%20H726%20(sun50iw15)-blue)
![FEL ID](https://img.shields.io/badge/FEL%20ID-0x1911-informational)
![Android](https://img.shields.io/badge/Android-14-green)
![Root](https://img.shields.io/badge/root-Magisk-success)
![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)
![Affiliation](https://img.shields.io/badge/affiliation-none-lightgrey)

<img src="assets/screenshots/home-3col.png" width="680" alt="Màn hình chính tùy biến">

</div>

---

## Repo này là gì

HY300 Ultra là máy chiếu **Android 14** hình trụ giá rẻ, bán dưới nhiều thương hiệu — Gojodoq, Magcubic... — nhưng chung **một bo mạch white-label Allwinner H726**. Khi mới mua, nó bị khóa chặt: đầy app rác quảng cáo/theo dõi, **backdoor họ BadBox nằm sẵn trong firmware**, auto-keystone chập chờn, userspace 32-bit từ chối nhiều app, và không có đường root hay chỉnh sửa chính thức.

Repo này là **bản ghi đầy đủ, tái lập được, của quá trình mổ xẻ một chiếc như vậy và biến nó thành của mình** — root, dịch ngược subsystem hiển thị/keystone, gỡ malware, và dựng lại phần mềm theo ý — với **mọi nguồn tham khảo đều được trích dẫn**.

Repo được viết để **bổ sung, không thay thế, [guide root trên XDA](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/)**. Guide đó chỉ *cách flash*; repo này ghi lại phần nó không có: mẹo checksum để image tùy biến flash được, ruột của hệ keystone, cách bóc malware, và bộ cấu hình hậu-root hoàn chỉnh.

**Dành cho ai:** người sở hữu máy chiếu HY300 chip **H726** muốn **hiểu và làm chủ** máy, chứ không chỉ làm theo. Tất cả đặc thù cho **H726 (sun50iw15 / FEL `0x1911`)**; máy H713 xem phần Tương thích bên dưới.

## Kết quả đạt được

| Mảng | Kết quả |
|---|---|
| **Root** | Thành công trên **H726 / sun50iw15** — theo hiểu biết của tôi là **bản root công khai đầu tiên** của SoC này trên máy chiếu dòng HY300 (mọi công cụ sẵn có đều cho H713). `adb shell su` trả về `uid=0(root) … u:r:magisk:s0`. Chìa khóa là sửa **checksum `add_sum` trong V-file** để PhoenixSuit chấp nhận image Magisk minimal-diff. → [docs/02](docs/02-rooting.md) |
| **Auto-keystone** | **Sửa dứt điểm.** Hình bị méo hóa ra không do prop nào, mà do **tư thế máy + chế độ chiếu**; sau khi calibrate một lần, ảnh **tự căn vuông ở mọi độ nghiêng và sống qua reboot**. → [docs/03](docs/03-keystone-display.md) |
| **Dịch ngược** | Dựng được toàn bộ **pipeline keystone / auto-keystone / autofocus** — hai đường keystone riêng, **bug dấu trục z** của gsensor, nơi lưu baseline thật (`set_angle_offset` → `/oem/gsensor_data`) — kèm **bảng khác biệt H726 vs H713**. Lần đầu RE công khai subsystem này cho H726. → [docs/03](docs/03-keystone-display.md) |
| **Bảo mật** | **Gỡ các thành phần BadBox 2.0** (Play Store giả/Tubesky, OTA dropper, telemetry) và **vô hiệu backdoor setuid-root `/system/xbin/hotacksu`**, giữ cho nó chết qua reboot bằng một module Magisk. → [docs/04](docs/04-debloat-security.md) |
| **Tùy biến** | **Màn hình chính** dựng lại (thời tiết, múi giờ thứ hai, âm lịch Việt, screensaver khung tranh tự đổi, header 3 cột), app **"Hub" gộp cài đặt**, boot logo riêng, múi giờ/nhãn máy. → [docs/05](docs/05-post-root.md) |

Mỗi kết quả đều kèm **những lỗi đã dẫn tới cách sửa**, để lập luận tái lập được chứ không chỉ là công thức.

## Trong repo có gì

- **[`docs/`](docs)** — các bài viết kỹ thuật (tiếng Anh; bản gốc tiếng Việt ở [`docs/vi/`](docs/vi)):
  [định danh phần cứng](docs/01-hardware-id.md) · [root + fix `add_sum`](docs/02-rooting.md) · [RE keystone/hiển thị](docs/03-keystone-display.md) · [debloat & gỡ BadBox](docs/04-debloat-security.md) · [tùy biến hậu-root](docs/05-post-root.md).
- **[`scripts/`](scripts)** — script build image root (`build_rooted_minimal.py`), helper phân tích SoC, và script khôi phục debloat.
- **[`launcher-mods/`](launcher-mods)** — patch (và bản copy) của màn hình chính tùy biến, trên nền [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
- **[`hub-app/`](hub-app)** — mã nguồn `com.hy300.hub`, launcher gộp cài đặt/keystone/casting.
- **[`magisk-module/`](magisk-module)** — `hy300tweaks`: vô hiệu backdoor + ép launcher tùy biến làm HOME.
- **[`assets/`](assets)** — boot logo và ảnh chụp màn.

> **Không kèm (có chủ đích):** image đã root ~2.2 GB, firmware stock, và mọi APK / `.so` / binary proprietary. GitHub không chứa nổi và chúng không thuộc quyền chia sẻ lại của tôi — script build sẽ tái tạo image từ firmware stock, và mọi binary đều link tới nguồn. Xem [Giấy phép](#giấy-phép).

## Tương thích — có phải máy của bạn?

| | Repo này (**H726**) | Không phải (**H713**) |
|---|---|---|
| SoC | `sun50iw15p1` (H726) | `sun50iw12` (H713) |
| FEL SoC id | **`0x1911`** | `0x1860` |
| Đường hiển thị | DRM/KMS thẳng (không MIPS) | MIPS co-proc (`display.bin`) |
| Cảm biến trọng lực | MPU-6880 | SC7A20 |

**Kiểm tra máy bạn:** `adb shell getprop ro.hardware` phải ra `sun50iw15p1`; trong chế độ FEL `sunxi-fel version` phải ra `soc=00001911`. Chi tiết: [docs/01](docs/01-hardware-id.md).

Nếu thấy `sun50iw12` / `0x1860` thì đó là **H713** — hãy dùng các dự án mà repo này dựa trên: [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), và [thread H713 trên XDA](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/).

## Miễn trừ

> **Tự chịu rủi ro.** Flash thiết bị Allwinner có thể **brick**; bạn tự chịu trách nhiệm với phần cứng, không bảo hành. Chỉ dành cho **H726** (xem Tương thích). Firmware stock là bản clone white-label thuộc họ **BigDroidOS / Bigpanzi** và bị gắn cờ **BadBox 2.0**; repo này chỉ ra cách phát hiện + vô hiệu, nhưng firmware clone không bao giờ tin tuyệt đối được — **đừng đăng nhập tài khoản cá nhân, ngân hàng hay Google, và hãy cô lập mạng cho máy.** Không redistribute firmware, APK hay thư viện proprietary.

## Bắt đầu

1. [Định danh phần cứng](docs/01-hardware-id.md) — xác nhận chip; các tên máy và cách vào FEL.
2. [Root](docs/02-rooting.md) — FEL + PhoenixSuit + fix `add_sum`; build image bằng [`scripts/build_rooted_minimal.py`](scripts/build_rooted_minimal.py).
3. [Debloat & bảo mật](docs/04-debloat-security.md) — gỡ rác và vô hiệu backdoor ([`magisk-module/hy300tweaks`](magisk-module/hy300tweaks)).
4. [Keystone & hiển thị](docs/03-keystone-display.md) — sửa auto-keystone (tư thế + chế độ chiếu + calibrate).
5. [Tùy biến hậu-root](docs/05-post-root.md) — launcher tùy biến ([`launcher-mods/`](launcher-mods)) và app Hub ([`hub-app/`](hub-app)).

## Lời cảm ơn

Công trình này dựa trên, và biết ơn:

- **guide root H726 trên XDA** — [*Rooting tutorial for a Magcubic HY300 Pro (Allwinner H726, Android 14)*](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/);
- **nền tảng research H713** — [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify), [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research), bản sunxi-tools / Linux + model keystone của well0nez, [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root);
- **tham chiếu gỡ BadBox** — [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal);
- và **[Claude](https://www.anthropic.com/claude) (AI của Anthropic), dùng như trợ lý nghiên cứu** xuyên suốt.

Phần việc mới ở đây — đường root `add_sum`, dịch ngược keystone/hiển thị, bóc BadBox và bộ hậu-root — được phát triển trên nền đó. Claude hỗ trợ như một công cụ; **không** là git co-author. Con người (chủ repo) ra quyết định, chịu rủi ro, và chạy phần cứng.

## Bài học — những lỗi đã dạy ra cách làm

- **Hex-edit thẳng vào IMAGEWTY thì hỏng** — container giữ checksum `add_sum` cho từng partition; đổi payload là phải tính lại V-file, không thì PhoenixSuit reject. *Đây chính là chìa khóa để root.*
- **Có hai đường keystone** — auto (gsensor → display engine, không đụng `persist.htc.keystone.*`) và manual (prop `htc.keystone`). Set tay khi auto bật sẽ bị đè — nên "lúc được lúc không".
- **"Hình thang ngược" là do tư thế, không phải prop** — đặt trụ máy logo-úp (gsensor `z < 0`) ép firmware vào nhánh "treo trần", đảo chiều bù + từ chối baseline. Lật logo-xuôi (`z > 0`) là chạy.
- **`persist.sys.panelvalue` chỉ là cache UI** — `setprop` + reboot không lật ảnh; phải gọi `factorySetPanelValue`.
- **Magic-mount của Magisk hỏng trên firmware này** — overlay `system/` không áp; dùng `/oem`, ghi thẳng partition, hoặc `service.d`.

## Cấu trúc repo

```
docs/            Bài viết kỹ thuật (tiếng Anh; bản gốc tiếng Việt ở docs/vi/)
scripts/         Script build image root + helper phân tích SoC (Python) + restore (PowerShell)
launcher-mods/   Patch + file cho màn hình chính tùy biến (trên nền Gathin23/cleanyourtv)
hub-app/         Mã app "Hub" (com.hy300.hub)
magisk-module/   hy300tweaks — vô hiệu backdoor hotacksu + ép launcher làm HOME
assets/          Boot logo + ảnh chụp màn
```

## Tham khảo

**Guides** — [Root H726 (XDA)](https://xdaforums.com/t/guide-rooting-tutorial-for-a-magcubic-hy300-pro-with-an-allwinner-h726-and-android-14.4776519/) · [Root H713 (XDA)](https://xdaforums.com/t/beta-android-tv-and-root-tutorial-for-allwinner-h713-based-projectors.4744020/)

**Research H713 (nền tảng)** — [probonopd/hy310-deshittify](https://github.com/probonopd/hy310-deshittify) · [YuujiLab/HY300-H713-Research](https://github.com/YuujiLab/HY300-H713-Research) · [gniax/magcubic-hy300-root](https://github.com/gniax/magcubic-hy300-root) · [schaumann-byte/hy300-projector-homeassistant](https://github.com/schaumann-byte/hy300-projector-homeassistant) · well0nez (sunxi-tools fork H713 + `allwinner-h713-linux`, model keystone)

**Bảo mật** — [maximilian-sh/hy350max-badbox-removal](https://github.com/maximilian-sh/hy350max-badbox-removal)

**Công cụ** — [Magisk](https://github.com/topjohnwu/Magisk) · [sunxi-tools](https://github.com/linux-sunxi/sunxi-tools) · [OpenixSuit](https://github.com/YuzukiTsuru/OpenixSuit) · [imagewty-tool](https://github.com/uictorius/imagewty-tool) · [MagiskPatcher](https://circlecashteam.github.io/MagiskPatcher/) · [jadx](https://github.com/skylot/jadx) (Apache-2.0) · PhoenixSuit (Allwinner)

**Phần mềm hậu-root** — [launcher cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT) · [SmartTube](https://github.com/yuliskov/SmartTube) · [Material Files](https://github.com/zhanghai/MaterialFiles) · Aurora Store · [Lucide icons](https://github.com/lucide-icons/lucide) · [Open-Meteo](https://open-meteo.com) · Outfit font (OFL)

**Firmware** (chỉ link, không redistribute) — androidpctv.com · magcubic-support.com. Ảnh nền do theme launcher kèm theo thuộc CC-BY / CC-BY-SA / CC0 của tác giả; xem `CREDITS.md` ở repo gốc.

## Giấy phép

[MIT](LICENSE) cho phần việc gốc trong repo. **Không** bao gồm các dự án upstream (mỗi dự án giữ license riêng) hay firmware/APK/thư viện proprietary — những thứ đó **không** có trong repo. Xem ghi chú phạm vi trong [LICENSE](LICENSE).

*Không liên kết với Allwinner, Magcubic, Gojodoq, Google hay Anthropic.*
