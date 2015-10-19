FROM localhost:5000/hadoop


#
## Install Kafka, Zookeeper and other needed things
#RUN curl http://www.us.apache.org/dist/kafka/0.8.1.1/kafka_2.10-0.8.1.1.tgz| tar -xzf - && \
#    mv kafka_2.10-0.8.1.1 /kafka && \
#    rm /var/cache/apk/* && \
#    mkdir /tmp/zookeeper && \
#    mkdir /tmp/kafka-logs
#
#ENV KAFKA_HOME /kafka
#ENV PATH $PATH:$KAFKA_HOME
#VOLUME ["/data"]
#WORKDIR /kafka
#ADD run.sh /kafka/run.sh
#RUN chmod a+x /kafka/run.sh
#
##ADD scripts/start-kafka.sh /usr/bin/start-kafka.sh
#
## Supervisor config
##ADD supervisor/kafka.conf /etc/supervisor/conf.d/kafka.conf
##ADD supervisor/zookeeper.conf /etc/supervisor/conf.d/zookeeper.conf
#
## 2181 is zookeeper, 9092 is kafka
#EXPOSE 2181 9092
#
##CMD ["supervisord", "-n"]
#CMD ["run.sh"]

ENV KAFKA_VERSION 0.8.1
ENV KAFKA_SCALA_VERSION 2.10
ENV DOWNLOAD_VERION kafka_$KAFKA_SCALA_VERSION-$KAFKA_VERSION
ENV KAFKA_TGZ_VERSION $DOWNLOAD_VERION.tgz
# Install Kafka, Zookeeper and other needed things
RUN wget -q https://archive.apache.org/dist/kafka/$KAFKA_VERSION/$KAFKA_TGZ_VERSION -O /tmp/$KAFKA_TGZ_VERSION && \
    tar xfz /tmp/$KAFKA_TGZ_VERSION -C /opt && \
    rm /tmp/$KAFKA_TGZ_VERSION

ENV KAFKA_HOME /opt/$DOWNLOAD_VERION
ADD scripts/start-kafka.sh /usr/bin/start-kafka.sh

# Supervisor config
ADD supervisor/kafka.conf /etc/supervisor/conf.d/kafka.conf
ADD supervisor/zookeeper.conf /etc/supervisor/conf.d/zookeeper.conf

ADD run.sh /usr/bin/run.sh

# 2181 is zookeeper, 9092 is kafka
EXPOSE 2181 9092

CMD ["bash","/usr/bin/run.sh"]
#CMD ["supervisord","-n"]
