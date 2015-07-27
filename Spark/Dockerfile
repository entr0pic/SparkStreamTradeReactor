FROM localhost:5000/hadoop

COPY . /base_files
RUN chmod -R 777 /base_files
# SPARK
ENV SPARK_VERSION 1.4.0
ENV HADOOP_VERSION 2.6
ENV SPARK_PACKAGE $SPARK_VERSION-bin-hadoop$HADOOP_VERSION
ENV SPARK_HOME /usr/spark-$SPARK_PACKAGE
ENV PATH $PATH:$SPARK_HOME/bin
RUN /base_files/get_spark.sh

COPY ./jars /jars

CMD /usr/spark/bin/spark-class org.apache.spark.deploy.master.Master

