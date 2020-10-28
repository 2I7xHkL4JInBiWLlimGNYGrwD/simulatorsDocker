FROM ubuntu:18.04

#RUN apt -y update
#RUN apt -y upgrade
#RUN apt -y install build-essential git m4 scons zlib1g zlib1g-dev \
#    libprotobuf-dev protobuf-compiler libprotoc-dev libgoogle-perftools-dev \
#    python-dev python python-six doxygen libboost-all-dev libhdf5-serial-dev \
#    python-pydot libpng-dev


COPY dependencies /dependencies
RUN dpkg -R --install --force-depends ./dependencies/
RUN rm -rf ./dependencies

## checkout repo with mercurial
#WORKDIR /usr/local/src
#RUN git clone https://github.com/gem5/gem5.git

COPY gem5 /usr/local/src/gem5


# build it
WORKDIR /usr/local/src/gem5
RUN scons build/X86/gem5.opt -j 1
