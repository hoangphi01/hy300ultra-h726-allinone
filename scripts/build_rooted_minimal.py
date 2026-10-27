#!/usr/bin/env python3
# MINIMAL-DIFF rooted image: copy the stock IMAGEWTY byte-for-byte, then overwrite
# ONLY the init_boot_a payload (Magisk) and vbmeta_a payload (verity disabled), both
# at their original offsets and EXACT original sizes. dlinfo, sys_partition, header,
# every other byte: untouched. So PhoenixSuit flashes it exactly like stock did
# (calibration/Reserve0 preserved the same way), but init_boot is now rooted.
import sys, shutil, os

SRC = sys.argv[1]   # stock .img
DST = sys.argv[2]   # output rooted .img
IB  = sys.argv[3]   # init_boot_magisk.img
VB  = sys.argv[4]   # vbmeta_disabled.fex

IB_OFF = 0x1120dc50; IB_LEN = 8388608
VB_OFF = 0x82443d70; VB_LEN = 8192

ib = open(IB, 'rb').read()
vb = open(VB, 'rb').read()
assert len(ib) == IB_LEN, ("init_boot size", len(ib), IB_LEN)
assert len(vb) == VB_LEN, ("vbmeta size",   len(vb), VB_LEN)
assert ib[:8] == b'ANDROID!', "init_boot not ANDROID!"
assert vb[:4] == b'AVB0',     "vbmeta not AVB0"

# sanity: stock must have ANDROID!/AVB0 exactly at those offsets (same-size swap)
with open(SRC, 'rb') as s:
    s.seek(IB_OFF); assert s.read(8) == b'ANDROID!', "stock IB_OFF mismatch"
    s.seek(VB_OFF); assert s.read(4) == b'AVB0',     "stock VB_OFF mismatch"

if os.path.exists(DST) and os.path.getsize(DST) == os.path.getsize(SRC):
    print("DST already right size, reusing copy")
else:
    print("copying stock image (2GB, a moment)...")
    shutil.copyfile(SRC, DST)

f = open(DST, 'r+b')
f.seek(IB_OFF); f.write(ib); print("init_boot_a  <= Magisk       (%d bytes @ 0x%x)" % (len(ib), IB_OFF))
f.seek(VB_OFF); f.write(vb); print("vbmeta_a     <= verity-off    (%d bytes @ 0x%x)" % (len(vb), VB_OFF))
f.close()

# verify only the two regions changed vs source size
assert os.path.getsize(DST) == os.path.getsize(SRC), "size changed!"
print("DONE (minimal diff, 2 regions only):", DST)
