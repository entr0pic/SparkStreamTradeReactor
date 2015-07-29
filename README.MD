Spark Trade Streaming
============

##Instructions

- Clone into a directory either under /Users/username or /Working (others are possible but you must edit the vagrantfile to add them as shared folders)
- Run ```./one_time_install.sh```
- This script will provide instructions along the way. (Beware will be downloading a few hundred mb)
- Recommended to edit your hosts file ```sudo vi /etc/hosts``` and add the VM IP addresss provided by the script using the name "vagrant"


##Usage

Start/Restart a VM with a cluster (includes nodes for Kafka, Zookeeper, Spark and node.js), this includes building packaged jars using SBT:

```shell
./startup.sh
```

Stop the VM:

```shell
./shutdown.sh
```

Restart the VM (usefull if you lose external internet access from the VM):

```shell
./restart.sh
```
##Kafka Stream

Not currently working properly.

To build (seperated from startup script):

```shell
cd SparkStreamingFromKafka && sbt package && sbt assembly
```