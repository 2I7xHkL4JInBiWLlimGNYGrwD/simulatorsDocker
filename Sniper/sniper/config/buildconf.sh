# This file is auto-generated, changes made to it will be lost. Please edit makebuildscripts.py instead.

if [ -z "${SNIPER_ROOT}" ] ; then SNIPER_ROOT=$(readlink -f "$(dirname "${BASH_SOURCE[0]}")/..") ; fi

DR_HOME=""
GRAPHITE_CC="cc"
GRAPHITE_CFLAGS="-mno-sse4 -mno-sse4.1 -mno-sse4.2 -mno-sse4a -mno-avx -mno-avx2 -I${SNIPER_ROOT}/include "
GRAPHITE_CXX="g++"
GRAPHITE_CXXFLAGS="-mno-sse4 -mno-sse4.1 -mno-sse4.2 -mno-sse4a -mno-avx -mno-avx2 -I${SNIPER_ROOT}/include "
GRAPHITE_LD="g++"
GRAPHITE_LDFLAGS="-static -L${SNIPER_ROOT}/lib -pthread "
GRAPHITE_LD_LIBRARY_PATH=""
GRAPHITE_UPCCFLAGS="-I${SNIPER_ROOT}/include  -link-with='g++ -static -L${SNIPER_ROOT}/lib -pthread'"
PIN_HOME="/usr/local/bin/pin-3.5-97503-gac534ca30-gcc-linux"
SNIPER_CC="cc"
SNIPER_CFLAGS="-mno-sse4 -mno-sse4.1 -mno-sse4.2 -mno-sse4a -mno-avx -mno-avx2 -I${SNIPER_ROOT}/include "
SNIPER_CXX="g++"
SNIPER_CXXFLAGS="-mno-sse4 -mno-sse4.1 -mno-sse4.2 -mno-sse4a -mno-avx -mno-avx2 -I${SNIPER_ROOT}/include "
SNIPER_LD="g++"
SNIPER_LDFLAGS="-static -L${SNIPER_ROOT}/lib -pthread "
SNIPER_LD_LIBRARY_PATH=""
SNIPER_UPCCFLAGS="-I${SNIPER_ROOT}/include  -link-with='g++ -static -L${SNIPER_ROOT}/lib -pthread'"
