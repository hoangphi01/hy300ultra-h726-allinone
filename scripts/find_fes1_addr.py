#!/usr/bin/env python3
import sys, struct
data = open(sys.argv[1], 'rb').read()

# 1) locate the fes1 format string
needle = b"down and run fes1 at addr"
si = data.find(needle)
print("fes1 string file-offset: 0x%x" % si)

# 2) scan for `push imm32` (0x68) where imm is in SRAM A1/A2 range
print("\npush imm32 of SRAM-range addresses (0x00100000..0x00140000):")
seen = {}
for i in range(len(data)-5):
    if data[i] == 0x68:
        v = struct.unpack('<I', data[i+1:i+5])[0]
        if 0x00100000 <= v <= 0x00140000:
            seen.setdefault(v, []).append(i)
for v in sorted(seen):
    offs = seen[v]
    print("  0x%08x  (x%d)  first@file 0x%x" % (v, len(offs), offs[0]))

# 3) also report pushes of SRAM range near typical boot0/fes staging & dram-status (+0x340)
print("\npush imm32 of any 0x001xxxxx (wider):")
seen2 = {}
for i in range(len(data)-5):
    if data[i] == 0x68:
        v = struct.unpack('<I', data[i+1:i+5])[0]
        if 0x00100000 <= v <= 0x001FFFFF:
            seen2[v] = seen2.get(v,0)+1
for v in sorted(seen2):
    print("  0x%08x x%d" % (v, seen2[v]))
