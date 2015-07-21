kafka_server=`dig kafka | grep SERVER | awk '{print $3}'| awk -F'#' '{print $1}'`

/usr/spark/bin/spark-submit --master spark://master:7077 /Working/HadoopHackathon/SparkStreamTradeReactor/SparkSteamingFromKafka/target/scala-2.10/tradeStream.jar $kafka:2181 trade-stream
