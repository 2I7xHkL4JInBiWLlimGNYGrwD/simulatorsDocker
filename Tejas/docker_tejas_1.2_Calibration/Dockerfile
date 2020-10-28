FROM ubuntu:18.04
MAINTAINER Sebastian Graef <sebastian@graef.me>

ENV SRC_PATH='/usr/local/src'
ENV BIN_PATH='/usr/local/bin'

## get dependencies
#RUN apt-get update && apt-get install -y \
# build-essential \
# git-core \
# openjdk-8-jdk \
# ant \
# make \
# python \
# g++ \
# gcc \
# zlib* \
# && rm -rf /var/lib/apt/lists/*
#RUN apt-get clean

COPY dependencies /dependencies
RUN dpkg -R --install --force-depends ./dependencies/
RUN rm -rf ./dependencies


# Setup Pintool
WORKDIR $BIN_PATH
COPY pin-3.7-97619-g0d0c92f4f-gcc-linux.tar.gz $BIN_PATH/pin-3.7-97619-g0d0c92f4f-gcc-linux.tar.gz
# Environment Variable PINPATH
ENV PINPATH $BIN_PATH/pin-3.7-97619-g0d0c92f4f-gcc-linux.tar.gz

# copy TEJAS
COPY Tejas_1.2.tar.gz $SRC_PATH/Tejas_1.2.tar.gz
ENV TEJAS_TAR_BALL $SRC_PATH/Tejas_1.2.tar.gz
WORKDIR $SRC_PATH

COPY install.py $SRC_PATH/install.py
RUN chmod 777 install.py && python install.py

COPY Calibration_1k_trace_file_0.gz $SRC_PATH/Tejas-Simulator/Tejas/scripts/trace_file_0.gz

# rm useless folder
RUN rm $SRC_PATH/Tejas_1.2.tar.gz && rm $BIN_PATH/pin-3.7-97619-g0d0c92f4f-gcc-linux.tar.gz

WORKDIR $SRC_PATH/Tejas-Simulator/Tejas

# java -jar jars/tejas.jar src/simulator/config/config.xml <output-file> <trace file>

# TEJAS simple test
WORKDIR $SRC_PATH/Tejas-Simulator/Tejas/scripts
#RUN ./test.sh

COPY example_config_4traces_L3.xml $SRC_PATH/Tejas-Simulator/Tejas/example_config_4traces.xml

# NEXT STEPS!
# > java -jar ../jars/tejas.jar ../example_config_4traces.xml out.txt trace_file
# HOST terminal
# > docker cp <ID>:/usr/local/src/Tejas-Simulator/Tejas/scripts/out.txt /Users/sebastian/Desktop/

ENTRYPOINT bash
