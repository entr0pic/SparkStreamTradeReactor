#docker push localhost:5000/base
#docker push localhost:5000/hadoop
#docker push localhost:5000/kafka
#docker push localhost:5000/spark
#docker push localhost:5000/tradegenerator

docker build -t localhost:5000/base Base/.
#docker push localhost:5000/base
docker build -t localhost:5000/hadoop Hadoop/.
#docker push localhost:5000/hadoop
docker build -t localhost:5000/kafka Kafka/.
#docker push localhost:5000/kafka
docker build -t localhost:5000/spark Spark/.
#docker push localhost:5000/spark
docker build -t localhost:5000/tradegenerator TradeGenerator/.
#docker push localhost:5000/tradegenerator
