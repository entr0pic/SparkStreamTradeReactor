FROM debian
RUN echo deb http://http.debian.net/debian jessie-backports main >> /etc/apt/sources.list.d/.list
RUN echo "deb http://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list

RUN apt-get update && apt-get -y --force-yes install sudo curl nodejs python sbt

RUN curl -sSL https://get.docker.com/ | sh
#RUN apt-get -t jessie-backports install -y "docker.io"

RUN curl -L https://bootstrap.pypa.io/get-pip.py > get-pip.py && python get-pip.py

RUN curl -L https://github.com/docker/compose/releases/download/1.3.3/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose && \
chmod +x /usr/local/bin/docker-compose

RUN pip install -U docker-compose

CMD ["bash"]
