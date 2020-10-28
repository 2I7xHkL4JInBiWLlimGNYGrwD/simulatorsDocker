FROM ubuntu:16.04
# Add i386 support for support for Pin
RUN dpkg --add-architecture i386
RUN apt-get update && apt-get install -y \
    python \
    screen \
    tmux \
    binutils \
    libc6:i386 \
    libncurses5:i386 \
    libstdc++6:i386 \
 && rm -rf /var/lib/apt/lists/*

COPY dependencies1 /dependencies1
RUN dpkg -R --install --force-depends ./dependencies1/
RUN rm -rf ./dependencies1


# For building Sniper
RUN apt-get update && apt-get install -y \
    automake \
    build-essential \
    curl \
    wget \
    libboost-dev \
    libsqlite3-dev \
    zlib1g-dev \
    libbz2-dev \
    g++-4.8 \
    g++-4.9 \
 && rm -rf /var/lib/apt/lists/*

COPY dependencies2 /dependencies2
RUN dpkg -R --install --force-depends ./dependencies2/
RUN rm -rf ./dependencies2


# For building RISC-V Tools ==> not needed
RUN apt-get update && apt-get  install -y \
    autoconf \
    automake \
    autotools-dev \
    bc \
    bison \
    curl \
    device-tree-compiler \
    flex \
    gawk \
    gperf \
    libexpat-dev \
    libgmp-dev \
    libmpc-dev \
    libmpfr-dev \
    libtool \
    libusb-1.0-0-dev \
    patchutils \
    pkg-config \
    texinfo \
    zlib1g-dev \
 && rm -rf /var/lib/apt/lists/*

COPY dependencies3 /dependencies3
RUN dpkg -R --install --force-depends ./dependencies3/
RUN rm -rf ./dependencies3


RUN apt-get update && apt-get install -y \
    git \
 && rm -rf /var/lib/apt/lists/*

COPY dependencies4 /dependencies4
RUN dpkg -R --install --force-depends ./dependencies4/
RUN rm -rf ./dependencies4


# Setup Pintool
ENV PIN_NAME='pin-3.5-97503-gac534ca30-gcc-linux'
COPY $PIN_NAME.tar.gz /usr/local/bin/$PIN_NAME.tar.gz
WORKDIR /usr/local/bin
RUN chmod ugo+x $PIN_NAME.tar.gz
RUN tar -xvzf $PIN_NAME.tar.gz
RUN rm /usr/local/bin/$PIN_NAME.tar.gz
RUN chmod 777 ./$PIN_NAME -R
# Environment Variable PINPATH
ENV PIN_HOME /usr/local/bin/$PIN_NAME

# Start Sniper installation
WORKDIR /usr/local/src/
#RUN git clone http://snipersim.org/download/217c2c1a0b688705/git/sniper.git
COPY sniper /usr/local/src/sniper
ENV SNIPER_PATH='/usr/local/src/sniper'
WORKDIR $SNIPER_PATH
RUN make
