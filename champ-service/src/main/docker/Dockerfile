FROM ubuntu:14.04

ARG MICRO_HOME=/opt/app/champ-service
ARG BIN_HOME=$MICRO_HOME/bin
ARG GRAPHLIB_HOME=$MICRO_HOME/graph-deps

RUN apt-get update

# Install and setup java8
RUN apt-get update && apt-get install -y software-properties-common
RUN sudo -E add-apt-repository ppa:openjdk-r/ppa && apt-get update && apt-get install -y openjdk-8-jdk
ENV JAVA_HOME usr/lib/jvm/java-8-openjdk-amd64
RUN export JAVA_HOME

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
ADD swm/package/nix/dist_files/appl/champ-service/* $MICRO_HOME/
RUN mkdir -p $GRAPHLIB_HOME
ADD graph-deps $GRAPHLIB_HOME
RUN mkdir -p $BIN_HOME
COPY *.sh $BIN_HOME
RUN chmod 755 $BIN_HOME/*
RUN ln -s /logs $MICRO_HOME/logs

# Create the aai user
RUN mkdir /opt/aaihome && \
    groupadd -g 492381 aaiadmin && \
    useradd -r -u 341790 -g 492381 -ms /bin/bash -d /opt/aaihome/aaiadmin aaiadmin && \
    chown -R aaiadmin:aaiadmin $MICRO_HOME
USER aaiadmin

EXPOSE 9522 9522

CMD ["/opt/app/champ-service/bin/start.sh"]
