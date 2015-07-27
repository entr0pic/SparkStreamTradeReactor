FROM debian:jessie

RUN apt-get update && apt-get install -y apt-utils zip unzip curl net-tools python wget ca-certificates supervisor dnsutils zookeeper npm openssh-server openssh-client rsync


ENV JDK_VERSION 8

# JAVA
ENV JAVA_HOME /usr/jdk1.8.0_31
ENV PATH $PATH:$JAVA_HOME/bin
RUN curl -sL --retry 3 --insecure \
  --header "Cookie: oraclelicense=accept-securebackup-cookie;" \
  "http://download.oracle.com/otn-pub/java/jdk/8u31-b13/server-jre-8u31-linux-x64.tar.gz" \
  | gunzip \
  | tar x -C /usr/ \
  && ln -s $JAVA_HOME /usr/java \
  && rm -rf $JAVA_HOME/man

RUN ln -s $JAVA_HOME $JAVA_HOME/default

RUN curl --silent --location https://deb.nodesource.com/setup_0.12 | bash - && \
    apt-get install -y nodejs build-essential
# Zookeeper manual
#RUN curl -sL --retry 3 --insecure \
#  "http://apache.mirror.digitalpacific.com.au/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz" \
#  | gunzip \
#  | tar x -C /usr/share \
#  && ln -s /usr/share/zookeeper-3.4.6 /usr/share/zookeeper

USER root

RUN rm /etc/ssh/ssh_host_dsa_key
RUN rm /etc/ssh/ssh_host_rsa_key
# passwordless ssh
RUN ssh-keygen -q -N "" -t dsa -f /etc/ssh/ssh_host_dsa_key
RUN ssh-keygen -q -N "" -t rsa -f /etc/ssh/ssh_host_rsa_key
RUN ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
RUN cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys
