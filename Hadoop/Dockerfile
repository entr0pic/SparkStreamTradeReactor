FROM localhost:5000/base

USER root
ENV DEBIAN_FRONTEND noninteractive

# Programs version
ENV HADOOP_VERSION 2.6.0

# Hadoop variable
ENV HADOOP_PREFIX /opt/hadoop
ENV HADOOP_CONF_DIR $HADOOP_PREFIX/conf
ENV PATH $PATH:$HADOOP_PREFIX/bin
ENV PATH $PATH:$HADOOP_PREFIX/sbin

# Install Hadoop
RUN wget http://archive.apache.org/dist/hadoop/core/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz && \
    tar -zxf /hadoop-$HADOOP_VERSION.tar.gz && \
    rm /hadoop-$HADOOP_VERSION.tar.gz && \
    mv hadoop-$HADOOP_VERSION $HADOOP_PREFIX && \
    mkdir -p $HADOOP_VERSION/logs

VOLUME /shared

ADD core-site.xml $HADOOP_CONF_DIR/core-site.xml
ADD hdfs-site.xml $HADOOP_CONF_DIR/hdfs-site.xml

RUN ssh-keygen -t dsa -P '' -f ~/.ssh/id_dsa && \
  cat ~/.ssh/id_dsa.pub >> ~/.ssh/authorized_keys && \
  export HADOOP\_PREFIX=/usr/local/hadoop

RUN echo Host * StrictHostKeyChecking no >> ~/.ssh/config

EXPOSE 50020 50090 50070 50010 50075 8031 8032 8033 8040 8042 49707 22 8088 8030
