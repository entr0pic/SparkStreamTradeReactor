import kafka.serializer.StringDecoder

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.sql._
import org.apache.spark.Logging

//import org.apache.log4j.{Level, Logger}

//object KafkaStream extends Logging{
//  def main(args: Array[String]) {
//    val conf = new SparkConf().setAppName("trade-reader")
//    val ssc = new StreamingContext(conf, Seconds(10))
//    ssc.checkpoint("hdfs://namenode:8020/checkpoint")
//
//    setStreamingLogLevels()
//
//    val Array(brokers, topic) = args
//
//    println(brokers)
//
//    val kafkaStream = KafkaUtils.createStream(ssc, brokers, "trade-generator", Map("trade-stream" -> 1))
//    kafkaStream.print()
//    val trades = kafkaStream.map(_._2)
//
//    val words = trades.flatMap(_.split(","))
//    words.saveAsTextFiles("hdfs://namenode:8020/store")
////    words.print()
//    val pairs = words.map(word => (word, 1))
//    val wordCounts = pairs.reduceByKey(_ + _)
//    wordCounts.print()
//    println("TEST")
//    println(trades)
//    println(words)
////    val wordCounts = words.map(x => (x, 1L))
////      .reduceByKeyAndWindow(_ + _, _ - _, Minutes(1), Seconds(10), 2)
////    wordCounts.print()
//
//    ssc.start()
//    ssc.awaitTermination()
//  }
//
//  /** Set reasonable logging levels for streaming if the user has not configured log4j. */
//    def setStreamingLogLevels() {
//      val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
//      if (!log4jInitialized) {
//        // We first log something to initialize Spark's default logging, then we override the
//        // logging level.
//        logInfo("Setting log level to [WARN] for streaming example." +
//          " To override add a custom log4j.properties to the classpath.")
//        Logger.getRootLogger.setLevel(Level.WARN)
//      }
//  }
//}



object DirectKafkaWordCount {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(s"""
        |Usage: DirectKafkaWordCount <brokers> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()

    val Array(brokers, topics) = args

    println(brokers)
    println(topics)
    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("DirectKafkaWordCount")
    //val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //val sqlContext = new SQLContext(sc)

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers, "auto.offset.reset" -> "smallest")
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)
    val path = "hdfs://namenode:8020/trade/store.parquet"

    // Get the lines, split them into words, count the words and print
    val trades = messages.map(_._2)

    trades.foreachRDD{rdd =>
      if (rdd.toLocalIterator.nonEmpty) {
        val sqlContext = new SQLContext(rdd.sparkContext)
        //val sqlContext = SQLContextSingleton.getInstance(rdd.sparkContext)
        import sqlContext.implicits._
        // Convert your data to a DataFrame, depends on the structure of your data
        //sqlContext.read.format("json").load("examples/src/main/resources/people.json")
        val df = sqlContext.jsonRDD(rdd).toDF
        df.save("org.apache.spark.sql.parquet", SaveMode.Append, Map("path" -> path))
      }
    }
    trades.print
//    val words = lines.flatMap(_.split(","))
//    val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
//    wordCounts.print()

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
