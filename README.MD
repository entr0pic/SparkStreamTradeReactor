Spark Trade Streaming
============


##Pre-Requisites

- install docker [https://docs.docker.com/installation/](https://docs.docker.com/installation/)
- install docker-compose [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)
- install node ```brew install node``` or [https://nodejs.org/download/](https://nodejs.org/download/)
- install sbt ```brew install sbt``` or [http://www.scala-sbt.org/download.html](http://www.scala-sbt.org/download.html)

##Usage

Start a cluster (includes nodes for Kafka, Zookeeper, Spark and node.js):

- ```docker-compose up -d ```

Destroy a cluster:

- ```docker-compose kill```

##Kafka Stream

Not currently working properly.

To build:

- ```sbt package```

To run:

- ```sbt run```


