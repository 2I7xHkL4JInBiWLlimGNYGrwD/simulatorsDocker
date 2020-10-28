FROM ubuntu:14.04
MAINTAINER Sebastian Graef <sebastian@graef.me>

## get dependencies
#RUN apt-get update
#RUN apt-get install -y build-essential g++ git-core scons gcc libelf-dev libconfig++-dev libconfig-dev libconfig-dbg libconfig++-dbg libhdf5-dev 
#RUN apt-get clean

COPY dependencies1 /dependencies1
RUN dpkg -R --install --force-depends ./dependencies1/
RUN rm -rf ./dependencies1


# to prevent issue: "gcc: error: /usr/include/asm/unistd.h"
RUN ln -s /usr/include/asm-generic /usr/include/asm

## install java for palladio code:
#RUN apt-get update
#RUN apt-get install -y software-properties-common debconf-utils

COPY dependencies2 /dependencies2
RUN dpkg -R --install --force-depends ./dependencies2/
RUN rm -rf ./dependencies2


#RUN add-apt-repository -y ppa:webupd8team/java
#RUN apt-get update
#RUN apt-get install -y openjdk-7-jre

COPY dependencies3 /dependencies3
RUN dpkg -R --install --force-depends ./dependencies3/
RUN rm -rf ./dependencies3


# Setup Pintool
COPY pin-2.14-71313-gcc.4.4.7-linux.tar.gz /usr/local/bin/pin-2.14-71313-gcc.4.4.7-linux.tar.gz
WORKDIR /usr/local/bin
RUN chmod ugo+x pin-2.14-71313-gcc.4.4.7-linux.tar.gz
RUN tar -xvzf pin-2.14-71313-gcc.4.4.7-linux.tar.gz
RUN rm /usr/local/bin/pin-2.14-71313-gcc.4.4.7-linux.tar.gz
RUN chmod 777 ./pin-2.14-71313-gcc.4.4.7-linux -R
# Environment Variable PINPATH
ENV PINPATH /usr/local/bin/pin-2.14-71313-gcc.4.4.7-linux

# checkout repo
WORKDIR /usr/local/src
# zsim for new hadware https://github.com/dzhang50/zsim-plusplus zsim
#RUN git clone https://github.com/Sebastian-G/zsim.git
COPY zsim/ /usr/local/src/zsim
WORKDIR /usr/local/src/zsim
#RUN git checkout linux-4.9

# Compile zsim
RUN scons -r

# Launch a test run
# RUN ./build/opt/zsim tests/simple.cfg

# Copy Java code dummy
WORKDIR /usr/local/bin
COPY JavaDemoTest.jar /usr/local/bin/JavaDemoTest.jar
WORKDIR /usr/local/src/zsim

ENTRYPOINT bash
