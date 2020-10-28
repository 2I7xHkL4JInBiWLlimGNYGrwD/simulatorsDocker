FROM ubuntu:14.04
MAINTAINER Sebastian Graef <sebastian@graef.me>

ENV SRC_PATH='/usr/local/src'
ENV BIN_PATH='/usr/local/bin'

# install java 6
RUN apt-get update && apt-get install -y \
    software-properties-common \
    && rm -rf /var/lib/apt/lists/*
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get update && apt-get install -y \
    openjdk-6-jre \
    openjdk-6-jdk \
    && rm -rf /var/lib/apt/lists/*

# install dependencies
RUN apt-get update && apt-get install -y \
    git-core \
    g++-multilib \
    gcc-multilib \
    ant \
    bison \
    perl \
    gawk \
    make \
    unzip \
    python \
    zlib* \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME /usr/lib/jvm/java-6-openjdk-amd64/jre
RUN java -version

# copy JikesRVM
WORKDIR $BIN_PATH
COPY jikesrvm-3.1.2.tar.gz $BIN_PATH/jikesrvm-3.1.2.tar.gz
RUN tar -xvzf jikesrvm-3.1.2.tar.gz && rm jikesrvm-3.1.2.tar.gz

# copy tejasjava
COPY tejasjava_installation_kit.tar.gz $BIN_PATH/tejasjava_installation_kit.tar.gz
RUN tar -xvzf tejasjava_installation_kit.tar.gz && rm tejasjava_installation_kit.tar.gz

# # ==> single-threaded
# RUN cp -R tejasjava_installation_kit/rvm  jikesrvm-3.1.2/rvm
# ==> multi-thread
RUN cp -R tejasjava_installation_kit/rvm_multithread  jikesrvm-3.1.2/rvm

RUN chmod 777 $BIN_PATH/ -R
WORKDIR $BIN_PATH/jikesrvm-3.1.2
# build develop
RUN bin/buildit localhost development
WORKDIR $BIN_PATH/jikesrvm-3.1.2/dist/development_x86_64-linux

# copy test .jar
COPY runablePrototype_Calibrated_16Core_Java6.jar $BIN_PATH/input.jar

# # generate trace_file from SimulatorBuild
# RUN ./rvm -jar $BIN_PATH/input.jar SimulatorBuild 2>trace_file
#
# WORKDIR $BIN_PATH/tejasjava_installation_kit/disas
# # Separate the instruction and memory traces
# RUN python conv.py $BIN_PATH/jikesrvm-3.1.2/dist/development_x86_64-linux/trace_file
#
# # Disassemble into x86 instructions
# RUN tar -xvzf udis86-1.7.2.tar.gz
# WORKDIR $BIN_PATH/tejasjava_installation_kit/disas/udis86-1.7.2
# RUN ./configure
# RUN make && make install
# WORKDIR $BIN_PATH/tejasjava_installation_kit/disas
# RUN g++ -std=c++11 disas.cpp -o disas.o -ludis86
# # requires temp_file.txt at the same directory
# RUN ./disas.o
#
# # Merge disassembled instruction traces and memory traces
# RUN javac conv1.java
# # generates fin_trace.txt_0
# RUN java conv1
#
# # compress all traces
# RUN gzip -c fin_trace.txt_0 >trace_file_0.gz
# # INFO: Compressed filename should end with _x.gz.
# # For single threaded program x should be 0 and for the multithreaded programs x is the thread number.
#
# #docker cp 294091bbfe05:/usr/local/bin/tejasjava_installation_kit/disas/trace_file_0.gz /Users/sebastian/Desktop/
#
# ENTRYPOINT bash
