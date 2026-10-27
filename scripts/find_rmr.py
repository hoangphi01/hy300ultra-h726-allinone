#!/usr/bin/env python3
import sys, struct
from capstone import *
d = open(sys.argv[1],'rb').read()
md = Cs(CS_ARCH_ARM, CS_MODE_ARM)
md.detail = True

def lit_at(off):
    if 0 <= off+4 <= len(d):
        return struct.unpack('<I', d[off:off+4])[0]
    return None

# disassemble whole thing as ARM, base=0
insns = list(md.disasm(d, 0))
# 1) find MCR/MRC with coproc register c12 (VBAR/RMR/MVBAR live in cp15 c12)
print("== MCR/MRC p15 cRx,c12,... (VBAR/RMR/MVBAR) ==")
for ins in insns:
    if ins.mnemonic in ('mcr','mrc') and 'c12' in ins.op_str:
        print("  0x%05x: %s %s" % (ins.address, ins.mnemonic, ins.op_str))

# 2) resolve ldr Rd,[pc,#imm] literal loads whose value is a CPU-subsystem MMIO (0x07..0x0A)
print("\n== ldr from literal -> CPU-range address (candidate RVBAR/CPUCFG) ==")
seen=set()
for ins in insns:
    if ins.mnemonic == 'ldr' and '[pc' in ins.op_str:
        # pc = addr+8 in ARM; imm in op_str
        try:
            imm = int(ins.op_str.split('#')[1].rstrip(']'),0)
        except:
            continue
        litoff = ins.address + 8 + imm
        v = lit_at(litoff)
        if v is not None and 0x07000000 <= v <= 0x0A000000:
            key=(ins.address,v)
            print("  0x%05x: %s   -> literal@0x%05x = 0x%08x" % (ins.address, ins.op_str, litoff, v))

# 3) look for the ATF/AArch64 entry constant (0x40000000 or similar) stored near a CPU-range address
print("\n== context: instructions around any 'str' following a CPU-range ldr ==")
for i,ins in enumerate(insns):
    if ins.mnemonic=='ldr' and '[pc' in ins.op_str:
        try: imm=int(ins.op_str.split('#')[1].rstrip(']'),0)
        except: continue
        v=lit_at(ins.address+8+imm)
        if v and 0x07000000<=v<=0x0A000000:
            # print next few insns
            ctx=insns[i:i+6]
            print("  --- near 0x%05x (addr 0x%08x) ---" % (ins.address, v))
            for c in ctx:
                print("    0x%05x: %-7s %s" % (c.address,c.mnemonic,c.op_str))
