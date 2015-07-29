FROM localhost:5000/spark

RUN apt-get update \
  && apt-get install -y git python python-setuptools python-dev python-numpy \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

# JAVA
#ENV JAVA_HOME /usr/jdk1.8.0_31
#ENV PATH $PATH:$JAVA_HOME/bin
#RUN curl -sL --retry 3 --insecure \
#  --header "Cookie: oraclelicense=accept-securebackup-cookie;" \
#  "http://download.oracle.com/otn-pub/java/jdk/8u31-b13/server-jre-8u31-linux-x64.tar.gz" \
#  | gunzip \
#  | tar x -C /usr/ \
#  && ln -s $JAVA_HOME /usr/java \
#  && rm -rf $JAVA_HOME/man
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

WORKDIR $ZEPPELIN_HOME

RUN mvn clean package -DskipTests \
  -Pspark-$SPARK_PROFILE \
  -Dspark.version=$SPARK_VERSION \
  -Phadoop-$HADOOP_PROFILE \
  -Dhadoop.version=$HADOOP_VERSION

RUN cat > $ZEPPELIN_HOME/conf/zeppelin-env.sh <<CONF
# There are several ways to configure Zeppelin
# 1. pass individual --environment variables during docker run
# 2. assign a volume and change the conf directory i.e.,
#    -e "ZEPPELIN_CONF_DIR=/zeppelin-conf" --volumes ./conf:/zeppelin-conf
# 3. when customizing the Dockerfile, add ENV instructions
# 4. write variables to zeppelin-env.sh during install.sh, as
#    we're doing here.
#
# See conf/zeppelin-env.sh.template for additional
# Zeppelin environment variables to set from here.
#

RUN rm -rf /root/.m2 \
  && rm -rf /root/.npm

ENV ZEPPELIN_MEM "-Xmx512m"
ENV ZEPPELIN_JAVA_OPTS "-Dspark.home=/usr/spark"

## INSTALL & CLEAN
#WORKDIR $ZEPPELIN_HOME
#ONBUILD COPY ./install.sh install.sh
#ONBUILD RUN ./install.sh \
#  && rm -rf /root/.m2 \
#  && rm -rf /root/.npm

CMD "bin/zeppelin.sh"
