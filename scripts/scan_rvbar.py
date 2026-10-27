#!/usr/bin/env python3
import sys, struct
d = open(sys.argv[1],'rb').read()
print("file size:", len(d))
# scan for 32-bit LE constants in CPU-subsystem MMIO range (RVBAR/CPUCFG candidates)
cand = {}
for i in range(0, len(d)-4, 4):
    v = struct.unpack('<I', d[i:i+4])[0]
    if 0x07000000 <= v <= 0x0A000000:
        cand.setdefault(v, []).append(i)
print("\n== CPU-subsystem address constants (0x07000000..0x0A000000) ==")
for v in sorted(cand):
    print("  0x%08x  x%d  @file %s" % (v, len(cand[v]), ','.join('0x%x'%o for o in cand[v][:4])))
# also look for RMR hints: MCR p15,0,Rx,c12,c0,2 encoding (ARM) = 0xEE0CxF50 pattern-ish; just report any 'RMR'/mcr near
# dump candidates ending in 0x40 or 0x00 (register-aligned) specially
print("\n== register-aligned (end 0x40/0x00/0x80) likely RVBAR ==")
for v in sorted(cand):
    if (v & 0xff) in (0x00,0x40,0x80,0xa0):
        print("  0x%08x" % v)
