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

//    println(brokers)
//    println(topics)
    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("TradeStreamReader")
    //val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //val sqlContext = new SQLContext(sc)

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers, "auto.offset.reset" -> "smallest")
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
    val trades = messages.map(_._2)
      
 val cleanData = trades.map(msg => {
                  val buffer:Array[Double] = new Array[Double](3)
        val src = msg.split(",")
//          buffer(0) = src(3)
//          buffer(1) = src(5)
//          buffer(2) = src(10)
//          val label = src(3)
//          (label, buffer)
        src
      })  
      
cleanData.foreach(println)

    trades.foreachRDD{rdd =>
      if (rdd.toLocalIterator.nonEmpty) {
//        val sqlContext = new SQLContext(rdd.sparkContext)
//           import sqlContext.implicits._
//       
//        // Convert your data to a DataFrame, depends on the structure of your data
//        val df = sqlContext.jsonRDD(rdd).toDF
//        df.save("org.apache.spark.sql.parquet", SaveMode.Append, Map("path" -> path))
          
//         val cleanData = df.select("category", "party_id", "counterparty_id", "currency_id").map{
//            row =>
//                val label = row.get(0);
//             println(row.get(0))
//             println(row.get(1))
//             println(row.get(2))
//             println(row.get(3))
//             
//                val buffer:Array[Double] = new Array[Double](3)
//                buffer(0) = row.get(1).toString.toDouble //get bank id
//                buffer(1) = row.get(2).toString.toDouble  //get counter party id
//                buffer(2) = row.get(3).toString.toDouble  // currency id
//
//             println(row)
//            val vector = Vectors.dense(buffer) 
//             (label,vector)
//          }
//         val data = cleanData.values.cache()
//        val numClusters = 34
//        val numIterations = 20
//          
//          val clusters = KMeans.train(data, numClusters, numIterations)
//  val WSSSE = clusters.computeCost(data)
//println("Within Set Sum of Squared Errors = " + WSSSE)
//println("clustering results:")        
//        clusteringScore(data, numClusters, numIterations).foreach(println)
          


      }
    }
      print ("Trades count ")
      trades.count().print
    
      
    /*
    import org.apache.spark.mllib.linalg._
    
    var rawData = // read the current message into this variable as row from parquet file
    var cleanData = rawData.map {
        row =>
            val label = row.get(2);
            val buffer = Array(
            row.get(1),//get bank id
            row.get(3),//get counter party id
            row.get(4),//get symbol id
            row.get(6),//get currency id
            );
    
            val vector = Vectors.dense(buffer.map(_.toDouble).toArray) (label,vector)
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

    }
    val data = cleanData.values.cache()
    
    import org.apache.spark.mllib.clustering._ 
    
    def distance(a: Vector, b: Vector) = math.sqrt(a.toArray.zip(b.toArray).map(p => p._1 - p._2).map(d => d * d).sum)

    def distToCentroid(datum: Vector, model: KMeansModel) = { 
        val cluster = model.predict(datum)
        val centroid = model.clusterCenters(cluster) distance(centroid, datum)
    }

import org.apache.spark.rdd._
def clusteringScore(data: RDD[Vector], k: Int, runs: Int) = { 
    val kmeans = new KMeans()
    kmeans.setK(k)
    kmeans.setRuns(runs)
    val model = kmeans.run(data)
    data.map(datum => distToCentroid(datum, model)).mean() 
}

    clusteringScore(data, 34, 10).foreach(println)
    
    */

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
