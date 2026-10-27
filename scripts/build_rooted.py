#!/usr/bin/env python3
# Build a SURGICAL rooted image: flash ONLY init_boot_a (Magisk) + vbmeta_a (no-verity).
# dlinfo rewritten to 2 entries so Reserve0/super/boot/etc. are NEVER flashed (calibration + data safe).
import sys, struct, shutil, os

SRC   = sys.argv[1]   # original .img
DST   = sys.argv[2]   # output rooted .img
IB    = sys.argv[3]   # init_boot_magisk.img
VB    = sys.argv[4]   # vbmeta_disabled.fex

# offsets/sizes from extraction
DLINFO_OFF = 0x0031e2b0; DLINFO_LEN = 16384
IB_OFF     = 0x1120dc50; IB_LEN     = 8388608
VB_OFF     = 0x82443d70; VB_LEN     = 8192

ib = open(IB,'rb').read(); vb = open(VB,'rb').read()
assert len(ib)==IB_LEN, (len(ib),IB_LEN)
assert len(vb)==VB_LEN, (len(vb),VB_LEN)
assert ib[:8]==b'ANDROID!', "init_boot not ANDROID!"
assert vb[:4]==b'AVB0', "vbmeta not AVB0"

if os.path.exists(DST) and os.path.getsize(DST)==os.path.getsize(SRC):
    print("DST exists with right size, skip copy")
else:
    print("copying image (2GB, a moment)...")
    shutil.copyfile(SRC, DST)

f = open(DST,'r+b')
# read original dlinfo
f.seek(DLINFO_OFF); dl = bytearray(f.read(DLINFO_LEN))
assert dl[:4]==b'\xef\xa4\x10\xcb', "dlinfo magic?"
count = struct.unpack('<I', dl[0x10:0x14])[0]
ENTRY = 0x48; BASE = 0x20
# find entry index by partition name
def find_entry(name):
    for i in range(count):
        off = BASE + i*ENTRY
        nm = dl[off:off+16].split(b'\x00')[0].decode('latin1')
        if nm == name:
            return bytes(dl[off:off+ENTRY])
    raise KeyError(name)
e_ib = find_entry('init_boot_a')
e_vb = find_entry('vbmeta_a')
# build surgical dlinfo: header + 2 entries + zeros
new = bytearray(DLINFO_LEN)
new[0:0x20] = dl[0:0x20]
struct.pack_into('<I', new, 0x10, 2)        # count = 2
new[BASE:BASE+ENTRY]            = e_ib
new[BASE+ENTRY:BASE+2*ENTRY]    = e_vb
f.seek(DLINFO_OFF); f.write(new)
print("dlinfo rewritten: count=2 (init_boot_a, vbmeta_a only). Reserve0 NOT listed.")

# patch partition data
f.seek(IB_OFF); f.write(ib); print("init_boot_a <= Magisk (%d bytes)" % len(ib))
f.seek(VB_OFF); f.write(vb); print("vbmeta_a <= no-verity (%d bytes)" % len(vb))
f.close()
print("DONE:", DST)
