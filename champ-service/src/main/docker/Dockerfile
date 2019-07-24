FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/champ-service
ARG BIN_HOME=$MICRO_HOME/bin
ARG GRAPHLIB_HOME=$MICRO_HOME/graph-deps

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
WORKDIR $MICRO_HOME
COPY /maven/champ-service/ .
RUN chmod 755 $BIN_HOME/* \
    && ln -snf /logs $MICRO_HOME/logs

EXPOSE 9522 9522

CMD ["/opt/app/champ-service/bin/start.sh"]
