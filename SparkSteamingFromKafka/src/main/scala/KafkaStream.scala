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

import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.stat.{MultivariateStatisticalSummary, Statistics}

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

def CreateDataArray(src: Map[String,Any]) : Array[Any] = {
        val buffer:Array[Any] = new Array[Any](7)
        buffer(0) = src("price")
        buffer(1) = src("party_weight")
        buffer(2) = src("exchange_weight")
        buffer(3) = src("currency_weight")
        buffer(4) = src("party")
        buffer(5) = src("exchange")
        buffer(6) = src("currency")
    buffer
}
      
def CreateEmptyArray() : Array[Any] = {
    val buffer: Array[Any] = new Array[Any](1)
    buffer(0) = 0.00
    buffer
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
      
//    messages.flatMap(case (_,line) => line).print()
    trades.foreachRDD{rdd =>
      if (rdd.toLocalIterator.nonEmpty) {
          //rdd.collect().take(10).foreach(a => println(a))
           val cleanData = rdd.collect().map{ line => 
                { 
                    JSON.parseFull(line)  match {
                        case None => CreateEmptyArray()
                        case Some( mapAsAny ) => mapAsAny match {
                            case x: Map[ String, Any ] => { CreateDataArray(x) }
                            case _ => CreateEmptyArray()
                        }
                    }
                }
          }.filter(_.size>1)
          
          val (left, right) = cleanData.splitAt(round(cleanData.size*0.9))
          
        val trainingData = left.map(_.take(4)).filter(_.size==4).map{ x => 
                Vectors.dense(x.map(_.toString.toDouble))
         }
          var  testingData = right.map(_.take(4)).filter(_.size==4).map{ x => 
              LabeledPoint(x(0).toString.toDouble, Vectors.dense(x.map(_.toString.toDouble)))
        }
    
        val summary: MultivariateStatisticalSummary = Statistics.colStats(trainingData)
      
println(summary.mean) // a dense vector containing the mean value for each column
println(summary.variance) // column-wise variance
println(summary.numNonzeros) // number of nonzeros in each column
  
      }
    }
      
//    val cleanData = messages.map{ case (_,line) => { 
//        JSON.parseFull(line)  match {
//            case None => CreateEmptyArray()
//            case Some( mapAsAny ) => mapAsAny match {
//                case x: Map[ String, Any ] => { CreateDataArray(x) }
//                case _ => CreateEmptyArray()
//            }
//        }
//    }
//  }.filter(_.size>1)
//    
//cleanData.print()

//println(cleanData.size)
//val (left, right) = cleanData.splitAt(round(cleanData/size*0.9))
      
  
//  val testingData = cleanData.map(_.take(4)).filter(_.size==4).map{ x => 
//          LabeledPoint(x(0).toString.toDouble, Vectors.dense(x.map(_.toString.toDouble)))
//    }
    
      

//trainingData.print()
//testingData.print()
      
      
//val summary: MultivariateStatisticalSummary = Statistics.colStats(trainingData)
//      
//println(summary.mean) // a dense vector containing the mean value for each column
//println(summary.variance) // column-wise variance
//println(summary.numNonzeros) // number of nonzeros in each column
      
      val numClusters = 2
      var numDimensions = 3
      
//    val model = new StreamingKMeans()
//      .setK(numClusters)
//    .setDecayFactor(1.0)
//      //.setHalfLife(halfLife, timeUnit)
//      .setRandomCenters(numDimensions, 0.0)
//
//    model.trainOn(trainingData)
//      val WSSSE = clusters.computeCost(trainingData)
//      println("Within Set Sum of Squared Errors = " + WSSSE)

//    model.predictOnValues(testingData).print()
//     

//    trades.foreachRDD{rdd =>
//      if (rdd.toLocalIterator.nonEmpty) {
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
//          
//      }
//    }
//      
    trades.count().print
      
    ssc.start()
    ssc.awaitTermination()
  }
}
