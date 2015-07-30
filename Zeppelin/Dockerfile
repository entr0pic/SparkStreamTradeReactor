FROM localhost:5000/spark

RUN apt-get update \
  && apt-get install -y git python python-setuptools python-dev python-numpy \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

RUN easy_install py4j

# MAVEN
ENV MAVEN_VERSION 3.3.1
ENV MAVEN_HOME /usr/apache-maven-$MAVEN_VERSION
ENV PATH $PATH:$MAVEN_HOME/bin
RUN curl -sL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
  | gunzip \
  | tar x -C /usr/ \
  && ln -s $MAVEN_HOME /usr/maven

# ZEPPELIN
ENV ZEPPELIN_HOME         /zeppelin
ENV ZEPPELIN_CONF_DIR     $ZEPPELIN_HOME/conf
ENV ZEPPELIN_NOTEBOOK_DIR $ZEPPELIN_HOME/notebook
ENV ZEPPELIN_PORT         8080

RUN git clone https://github.com/apache/incubator-zeppelin.git $ZEPPELIN_HOME
COPY ./install.sh $ZEPPELIN_HOME/install.sh
COPY ./.m2 /root/.m2

WORKDIR $ZEPPELIN_HOME

RUN mvn clean package -DskipTests \
  -Pspark-$SPARK_PROFILE \
  -Dspark.version=$SPARK_VERSION \
  -Phadoop-$HADOOP_PROFILE \
  -Dhadoop.version=$HADOOP_VERSION

RUN cat > $ZEPPELIN_HOME/conf/zeppelin-env.sh <<CONF

RUN rm -rf /root/.m2 \
  && rm -rf /root/.npm


ENV ZEPPELIN_MEM "-Xmx512m"
ENV ZEPPELIN_JAVA_OPTS "-Dspark.home=/usr/spark"

CMD "bin/zeppelin.sh"
