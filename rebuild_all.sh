docker build -t localhost:5000/base Base/.
docker build -t localhost:5000/kafka Kafka/.
docker build -t localhost:5000/spark Spark/.
docker build -t localhost:5000/tradegenerator TradeGenerator/.

docker push localhost:5000/base
docker push localhost:5000/kafka
docker push localhost:5000/spark
docker push localhost:5000/tradegenerator
