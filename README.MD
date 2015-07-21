Spark Trade Streaming
============


##Instructions

- Clone into a directory either under /Users/username or /Working (others are possible but you must edit the vagrantfile to add them as shared folders)
- Run ```sh one_time_install.sh```
- This script will provide instructions along the way. (Beware will be downloading a few hundred mb)
- Recommended to edit your hosts file ```sudo vi /etc/hosts``` and add the vagrant IP addresss provided by the script using the name "vagrant"


##Usage

Start a cluster (includes nodes for Kafka, Zookeeper, Spark and node.js):

- ```docker-compose up -d ```

Destroy a cluster:

- ```docker-compose kill```

##Kafka Stream

Not currently working properly.

To build:

- ```cd SparkStreamingFromKafka && sbt package && sbt assembly```

To run:

- Start main containers (```docker-compose up -d ```)
- ```cd SparkStreamingFromKafka```
- ```docker run -it -v `pwd`:`pwd` -e "projectDir=`pwd`" --link sparkstreamtradereactor_master_1:master --link sparkstreamtradereactor_kafka_1:kafka localhost:5000/spark bash -c "base_files/trade-stream.sh"```