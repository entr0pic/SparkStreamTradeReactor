FROM localhost:5000/spark

RUN apt-get update \
  && apt-get install -y git python python-setuptools python-dev python-numpy \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

RUN easy_install py4j


# ZEPPELIN
ENV ZEPPELIN_HOME         /zeppelin
ENV ZEPPELIN_CONF_DIR     $ZEPPELIN_HOME/conf
ENV ZEPPELIN_NOTEBOOK_DIR $ZEPPELIN_HOME/notebook
ENV ZEPPELIN_PORT         8080


COPY ./install.sh install.sh
COPY ./build_files /build_files

RUN ./install.sh

WORKDIR $ZEPPELIN_HOME

RUN cat > $ZEPPELIN_HOME/conf/zeppelin-env.sh <<CONF

ENV ZEPPELIN_MEM "-Xmx512m"
ENV ZEPPELIN_JAVA_OPTS "-Dspark.home=/usr/spark"

CMD "bin/zeppelin.sh"
