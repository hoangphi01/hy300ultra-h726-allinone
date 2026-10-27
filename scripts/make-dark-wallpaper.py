#!/usr/bin/env python3
"""
Generate a dark Material background for MagcubicOS and (optionally) print the adb
commands to install it. MagcubicOS draws every settings screen on top of one shared
bitmap, MyApplication.mainDrawable, loaded at process start from:

    /data/user_de/0/com.htc.magcubicos/files/.wallpaper/background_main.png

Dropping a dark 1280x720 PNG there recolors the whole vendor settings UI from its
harsh royal blue to a dark surface at once, with no overlay, no app repackaging and
no re-signing. No third-party image library needed: this writes the PNG by hand with
the standard library (zlib + struct).

Usage:
    python make-dark-wallpaper.py            # writes background_main.png here
    python make-dark-wallpaper.py out.png
"""
import sys, zlib, struct

W, H = 1280, 720
TOP = (18, 24, 36)   # #121824
BOT = (8, 11, 17)    # #080B11


def build_png(path):
    raw = bytearray()
    for y in range(H):
        t = y / (H - 1)
        r = int(TOP[0] + (BOT[0] - TOP[0]) * t)
        g = int(TOP[1] + (BOT[1] - TOP[1]) * t)
        b = int(TOP[2] + (BOT[2] - TOP[2]) * t)
        raw.append(0)                       # PNG filter type 0 (none) per scanline
        raw.extend(bytes((r, g, b)) * W)

    def chunk(typ, data):
        return (struct.pack(">I", len(data)) + typ + data +
                struct.pack(">I", zlib.crc32(typ + data) & 0xffffffff))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 2, 0, 0, 0))  # 8-bit RGB
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "background_main.png"
    build_png(out)
    print("wrote", out, "(%dx%d)" % (W, H))
    print()
    print("Install (device must be rooted):")
    print("  adb push %s /data/local/tmp/background_main.png" % out)
    print("  adb shell su -c 'D=/data/user_de/0/com.htc.magcubicos/files/.wallpaper; \\")
    print("    mkdir -p $D; cp /data/local/tmp/background_main.png $D/; \\")
    print("    chown $(stat -c %u:%g /data/user_de/0/com.htc.magcubicos) $D/background_main.png; \\")
    print("    chmod 644 $D/background_main.png; restorecon -R $D; am force-stop com.htc.magcubicos'")
