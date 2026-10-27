#!/usr/bin/env python3
# Edit sys_partition.fex IN-PLACE inside the rooted image: keep downloadfile only for
# init_boot_a and vbmeta_a; drop it for every other partition. Belt-and-suspenders so
# Reserve0 (per-unit calibration) is never flashed regardless of what the tool reads.
import sys, re
IMG = sys.argv[1]
SP_OFF = 0x00011970; SP_LEN = 4944
KEEP = {'init_boot_a', 'vbmeta_a'}

f = open(IMG, 'r+b')
f.seek(SP_OFF); raw = f.read(SP_LEN)
text = raw.decode('latin1')

out_lines = []
cur = None
removed = []
for line in text.split('\n'):
    s = line.strip()
    m = re.match(r'name\s*=\s*([A-Za-z0-9_]+)', s)
    if m and not s.startswith(';'):
        cur = m.group(1)
    if re.match(r'downloadfile\s*=', s) and not s.startswith(';'):
        if cur not in KEEP:
            removed.append(cur)
            continue  # drop this downloadfile line
    out_lines.append(line)

new = '\n'.join(out_lines).encode('latin1')
assert len(new) <= SP_LEN, (len(new), SP_LEN)
new = new + b'\x00' * (SP_LEN - len(new))   # pad to original stored size
f.seek(SP_OFF); f.write(new)
f.close()
print("sys_partition patched. downloadfile removed for:", ', '.join(removed))
print("kept downloadfile for:", ', '.join(sorted(KEEP)))
