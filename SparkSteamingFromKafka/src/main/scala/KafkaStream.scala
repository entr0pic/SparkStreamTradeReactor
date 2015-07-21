import kafka.serializer.StringDecoder

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._


object KafkaStream {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("trade-reader")
    val ssc = new StreamingContext(conf, Seconds(10))
    val Array(brokers, topic) = args

    val kafkaStream = KafkaUtils.createStream(ssc, brokers, "trade-generator", Map(topic -> 1))
    val trades = kafkaStream.map(_._2)
    println(trades)

    ssc.start()
    ssc.awaitTermination()
  }

}
//
//
//object KafkaStream{
//
//  def createStream[K: ClassTag, V: ClassTag, U <: Decoder[_]: ClassTag, T <: Decoder[_]: ClassTag](
//      ssc: StreamingContext,
//      kafkaParams: Map[String, String],
//      topics: Map[String, Int],
//      storageLevel: StorageLevel
//    ): ReceiverInputDStream[(K, V)]
//
//  val Array(zkQuorum, topics) = args
//
//  val kafkaParams = Map[String, String](
//      "zookeeper.connect" -> zkQuorum, "group.id" -> groupId,
//      "zookeeper.connection.timeout.ms" -> "10000",
//      "kafka.auto.offset.reset" -> "smallest"
//  )
//}
//
//
//object KafkaStream{
//  def main(args: Array[String]) {
//    val conf = new SparkConf().setAppName("trade-reader")
//    val ssc = new StreamingContext(conf, Seconds(10))
//    val Array(brokers, topics) = args
//    //val topics = "trade-stream"
//    //val brokers = "kafka:2181"
//    val topicsSet = topics.split(",").toSet
//    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
//    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
//      ssc, kafkaParams, topicsSet)
//
//    // Get the lines, split them into words, count the words and print
//    val lines = messages.map(_._2)
////    val words = lines.flatMap(_.split(" "))
////    val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
//
////    println(lines.size + " Trades")
//    lines.print()
//
//    // Start the computation
//    ssc.start()
//    ssc.awaitTermination()
//  }
//}

//object KafkaWordCount {
//  def main(args: Array[String]) {
//    if (args.length < 4) {
//      System.err.println("Usage: KafkaWordCount <zkQuorum> <group> <topics> <numThreads>")
//      System.exit(1)
//    }
//
//    StreamingExamples.setStreamingLogLevels()
//
//    val Array(zkQuorum, group, topics, numThreads) = args
//    val sparkConf = new SparkConf().setAppName("KafkaWordCount")
//    val ssc = new StreamingContext(sparkConf, Seconds(2))
//    ssc.checkpoint("checkpoint")
//
//    val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap
//    val lines = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
//    val words = lines.flatMap(_.split(" "))
//    val wordCounts = words.map(x => (x, 1L))
//      .reduceByKeyAndWindow(_ + _, _ - _, Minutes(10), Seconds(2), 2)
//    wordCounts.print()
//
//    ssc.start()
//    ssc.awaitTermination()
//  }
//}

