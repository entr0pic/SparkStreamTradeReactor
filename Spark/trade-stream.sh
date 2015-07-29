kafka_server=`dig kafka | grep SERVER | awk '{print $3}'| awk -F'#' '{print $1}'`

echo "Waiting for nodes to start up"
sleep 5
echo Connecting to $kafka_server:$KAFKA_PORT
sleep 5
echo Reading topic $KAFKA_TOPIC
sleep 5
echo Submitting to spark...
/usr/spark/bin/spark-submit --master spark://master:7077 /jars/tradeStream.jar "kafka:$KAFKA_PORT" $KAFKA_TOPIC
