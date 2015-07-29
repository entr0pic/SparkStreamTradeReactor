kafka_server=`dig kafka | grep SERVER | awk '{print $3}'| awk -F'#' '{print $1}'`

echo "Waiting for nodes to start up"
sleep 20

/usr/spark/bin/spark-submit --master spark://master:7077 /jars/tradeStream.jar $kafka_server:9092 trade-stream
