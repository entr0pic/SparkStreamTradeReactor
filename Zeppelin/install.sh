#!/bin/bash

if [ ! -f /build_files/zeppelin.tar.gz ]; then
  git clone https://github.com/apache/incubator-zeppelin.git $ZEPPELIN_HOME

  # MAVEN
  export MAVEN_VERSION=3.3.1
  export MAVEN_HOME=/usr/apache-maven-$MAVEN_VERSION
  export PATH=$PATH:$MAVEN_HOME/bin

  curl -sL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | gunzip \
    | tar x -C /usr/ \
    && ln -s $MAVEN_HOME /usr/maven

  git pull

  mvn clean package -DskipTests \
    -Pspark-$SPARK_PROFILE \
    -Dspark.version=$SPARK_VERSION \
    -Phadoop-$HADOOP_PROFILE \
    -Dhadoop.version=$HADOOP_VERSION

  rm -rf /root/.m2 \
  && rm -rf /root/.npm
else
   tar -xzvf  /build_files/zeppelin.tar.gz
fi





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
