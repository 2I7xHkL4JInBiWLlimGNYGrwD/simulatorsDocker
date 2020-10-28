FROM ubuntu:14.04
LABEL maintainer="Samuel Dettenmaier"

ENV SRC_PATH='/usr/local/src'
ENV BIN_PATH='/usr/local/bin'

## install java 6
#RUN apt-get update && apt-get install -y \
#    software-properties-common \
#    && rm -rf /var/lib/apt/lists/*

COPY dependencies1 /dependencies1
RUN dpkg -R --install --force-depends ./dependencies1/
RUN rm -rf ./dependencies1


#RUN add-apt-repository -y ppa:webupd8team/java
#RUN apt-get update && apt-get install -y \
#    openjdk-6-jre \
#    openjdk-6-jdk \
#    && rm -rf /var/lib/apt/lists/*
#
COPY dependencies2 /dependencies2
RUN dpkg -R --install --force-depends ./dependencies2/
RUN rm -rf ./dependencies2


## install dependencies
#RUN apt-get update && apt-get install --download-only -y \
#    git-core \
#    g++-multilib \
#    gcc-multilib \
#    ant \
#    bison \
#    perl \
#    gawk \
#    make \
#    unzip \
#    python \
#    zlib* \
#    && rm -rf /var/lib/apt/lists/*

COPY dependencies3 /dependencies3
RUN dpkg -R --install --force-depends ./dependencies3/
RUN rm -rf ./dependencies3



ENV JAVA_HOME /usr/lib/jvm/java-6-openjdk-amd64/jre
RUN java -version

# copy JikesRVM
WORKDIR $BIN_PATH
COPY development_x86_64-linux_multithread $BIN_PATH/development_x86_64-linux_multithread
