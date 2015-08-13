import kafka.serializer.StringDecoder

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.sql._
import org.apache.spark.Logging
 
import org.apache.spark.mllib._

import org.apache.spark.mllib.clustering._
import org.apache.spark.mllib.linalg._
import org.apache.spark.rdd._

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.clustering.StreamingKMeans

import scala.util.parsing.json._



//import org.apache.log4j.{Level, Logger}

/*
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
*/

object TradeStreamReader {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(s"""
        |Usage: TradeStreamReader <brokers> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()

    val Array(brokers, topics, path) = args
//    println(brokers)
//    println(topics)

/*
//    def distance(a: Vector, b: Vector) = math.sqrt(a.toArray.zip(b.toArray).map(p => p._1 - p._2).map(d => d * d).sum)
//
//    def distToCentroid(datum: Vector, model: KMeansModel) = { 
//        val cluster = model.predict(datum)
//        val centroid = model.clusterCenters(cluster) 
//        distance(centroid, datum)
//    }
//
//def clusteringScore(data: RDD[Vector], k: Int, runs: Int) = { 
//    val kmeans = new KMeans()
//    kmeans.setK(k)
//    kmeans.setRuns(runs)
//    val model = kmeans.run(data)
//    data.map(datum => distToCentroid(datum, model))
//    
//}
*/
      

    def preformatForDouble(src:String):String = {
        var ret = src.split(":")(1)
        ret = ret.substring(1, ret.length-2).toString.replace("^[0]+", "")//.replace("^[.]{1}", "0.")
        if (ret.indexOf(".")==ret.lastIndexOf(".")) ret else ret.substring(0, ret.lastIndexOf(".")-1)
//        val parts = ret.split(".")
//        if (parts.length>0) parts(0)+"."+parts(1) else ret
    }

def CreateDataArray(src: Any) : Any = {
    src
    //        val buffer:Array[Any] = new Array[Any](7)
//        buffer(0) = src("price")
//        buffer(1) = src("party_weight")
//        buffer(2) = src("country_weight")
//        buffer(3) = src("currency_weight")
//        buffer(4) = src("party")
//        buffer(5) = src("country")
//        buffer(6) = src("currency")
//    (buffer)
}
      
    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("TradeStreamReader")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //val sqlContext = new SQLContext(sc)

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers, "auto.offset.reset" -> "smallest")
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
     val trades = messages.map(_._2)
      
      
 val cleanData1 = messages.map{
//     case(_,line) => line.split(",")//.map(x => x.split(":"))
     case (_,line) => {
         JSON.parseFull(line) match {
            case Some(x) => x
         }
     }
 }
    
//cleanData1.print()
val cleanData = cleanData1.map(x => CreateDataArray(x)).print()
      
//val trainingData = cleanData.map(_.take(4)).flatMap(x => x.map(_.toDouble))//.map(Vectors.parse)
//trainingData.print()

//      var testingData = cleanData.map(l => LabeledPoint(l(0), l)).map(LabeledPoint.parse)
//      var trainingData = cleanData.map(x => Vectors.parse(x))
//
//      val numClusters = 34
//      var numDimensions = 3
//      
//    val model = new StreamingKMeans()
//      .setK(numClusters)
//    .setDecayFactor(1.0)
//      //.setHalfLife(halfLife, timeUnit)
//      .setRandomCenters(numDimensions, 0.0)

//    model.trainOn(trainingData)
//    model.predictOnValues(testingData).print()
     

    trades.foreachRDD{rdd =>
      if (rdd.toLocalIterator.nonEmpty) {
//        val sqlContext = new SQLContext(rdd.sparkContext)
//           import sqlContext.implicits._
//       
//        // Convert your data to a DataFrame, depends on the structure of your data
//        val df = sqlContext.jsonRDD(rdd).toDF
//        df.save("org.apache.spark.sql.parquet", SaveMode.Append, Map("path" -> path))
//          
//          val clusters = KMeans.train(data, numClusters, numIterations)
//  val WSSSE = clusters.computeCost(data)
//println("Within Set Sum of Squared Errors = " + WSSSE)
//println("clustering results:")        
//        clusteringScore(data, numClusters, numIterations).foreach(println)
          
      }
    }
      
      trades.count().print
    
      
//            
//                  trade_date: dateTime[0],
//      trade_time: startDate.toISOString(),
//      party: bank[0].swift,
//      party_id: party_id,
//      counterparty: bank[1].swift,
//      counterparty_id: counterparty_id,
//      ccp: ccp[0].BICCode,
//      exchange: symbol[0].Exchange,
//      symbol: symbol[0].Symbol,
//      currency: symbol[0].Currency,
//        currency_id : currency_id,
//      side: side?'B':'S',
//      type: symbol[0].Type,
//      category: symbol[0].Category,
//      price: price,
//      volume: volume,
//      unit: symbol[0].Unit

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
