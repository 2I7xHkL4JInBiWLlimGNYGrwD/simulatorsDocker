#!/bin/bash
# Build and Clean in one step to keep image manageable

target_ISA='X86'
echo 'building target system: ' $target_ISA '!'

scons -j$(nproc) --ignore-style build/$target_ISA/gem5.opt
rm -f /usr/local/bin/gem5.opt
mv build/$target_ISA/gem5.opt /usr/local/bin
rm -rf build
mkdir -p build/$target_ISA
ln -s /usr/local/bin/gem5.opt build/$target_ISA/gem5.opt

