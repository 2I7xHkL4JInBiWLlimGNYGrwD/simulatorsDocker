FROM ubuntu:14.04
MAINTAINER Sebastian Graef <sebastian@graef.me>

# get dependencies
#RUN apt-get update
#RUN apt-get install --download-only -y build-essential git-core automake autoconf libtool zlib1g-dev lib32gcc1 gcc-multilib libgtk-3-dev
#RUN apt-get clean

COPY dependencies /dependencies
RUN dpkg -R --install --force-depends ./dependencies/
RUN rm -rf ./dependencies


# checkout repo
WORKDIR /usr/local/src
#RUN git clone https://github.com/Multi2Sim/multi2sim.git
COPY multi2sim /usr/local/src/multi2sim
WORKDIR /usr/local/src/multi2sim

# Build Multi2Sim
RUN libtoolize
RUN aclocal
RUN autoconf
RUN automake --add-missing
RUN ./configure
RUN make
RUN make install

# Build test
# m2s
WORKDIR /usr/local/src/multi2sim/samples/x86/example-1
RUN m2s test-args how are you

ENTRYPOINT bash
