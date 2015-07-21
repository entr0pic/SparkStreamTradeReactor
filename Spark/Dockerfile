FROM localhost:5000/base

## JAVA
#ENV JAVA_HOME /usr/jdk1.8.0_31
#ENV PATH $PATH:$JAVA_HOME/bin
#RUN curl -sL --retry 3 --insecure \
#  --header "Cookie: oraclelicense=accept-securebackup-cookie;" \
#  "http://download.oracle.com/otn-pub/java/jdk/8u31-b13/server-jre-8u31-linux-x64.tar.gz" \
#  | gunzip \
#  | tar x -C /usr/ \
#  && ln -s $JAVA_HOME /usr/java \
#  && rm -rf $JAVA_HOME/man

COPY . /base_files
RUN chmod -R 777 /base_files
# SPARK
ENV SPARK_VERSION 1.4.0
ENV HADOOP_VERSION 2.6
ENV SPARK_PACKAGE $SPARK_VERSION-bin-hadoop$HADOOP_VERSION
ENV SPARK_HOME /usr/spark-$SPARK_PACKAGE
ENV PATH $PATH:$SPARK_HOME/bin
RUN /base_files/get_spark.sh

CMD /usr/spark/bin/spark-class org.apache.spark.deploy.master.Master

