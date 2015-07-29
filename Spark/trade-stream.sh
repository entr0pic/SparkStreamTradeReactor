kafka_server=`dig kafka | grep SERVER | awk '{print $3}'| awk -F'#' '{print $1}'`

echo "Waiting for nodes to start up"
sleep 5
echo Connecting to $kafka_server:9092
sleep 5
echo Reading topic trade-stream
sleep 5

/usr/spark/bin/spark-submit --master spark://master:7077 /jars/tradeStream.jar "$kafka_server:2181" "trade-stream"
