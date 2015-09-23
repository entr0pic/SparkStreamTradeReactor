Spark Trade Streaming
============

##Instructions

- Ideally you will want to set set this up on a mid to high spec machine with docker and docker compose already installed and you can follow

###Using VMs (not recommended)
- Clone into a directory either under /Users/username or /Working (others are possible but you must edit the vagrantfile to add them as shared folders)
- Run ```./one_time_install.sh```
- This script will provide instructions along the way. (Beware will be downloading a few hundred mb)
- Recommended to edit your hosts file ```sudo vi /etc/hosts``` and add the VM IP addresss provided by the script using the name "vagrant"


##Usage

Start/Restart a cluster (includes nodes for Kafka, Zookeeper, Spark and node.js), this includes building packaged jars using SBT:

```shell
sh sbt_rebuild.sh
sh rebuild_all.sh
rm -R Hadoop/data/datanode1/*
rm -R Hadoop/data/datanode2/*
rm -R Hadoop/data/namenode/*
sh reformat_hdfs.sh
docker-compose up -d
```

##Kafka Stream

Not currently working properly.

To build (seperated from startup script):

```shell
cd SparkStreamingFromKafka && sbt package && sbt assembly
```