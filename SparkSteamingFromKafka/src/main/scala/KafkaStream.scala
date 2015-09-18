import kafka.serializer.StringDecoder
import java.io.IOException

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

//--
import scala.reflect.ClassTag

import org.apache.spark.Logging
import org.apache.spark.SparkContext._
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.mllib.linalg.{BLAS, Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.util.Utils
import org.apache.spark.util.random.XORShiftRandom

import java.util.HashMap
import org.apache.kafka.clients.producer.{ProducerConfig, KafkaProducer, ProducerRecord}

//--

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


//------------------------ init variables --------------
      
    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("TradeStreamReader")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //val sqlContext = new SQLContext(sc)

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers, "auto.offset.reset" -> "smallest")
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics.split(",").filter(_=="trades").toSet)
    val tmessages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics.split(",").filter(_=="ttrades").toSet)

    val trades = messages.map(_._2)
    val ttrades = tmessages.map(_._2)
      
val numDimensions = 3
val numClusters = 2
val decayFactor = 1.0
      
val sModel = new StreamingKMeans()
  .setK(numClusters)
  .setDecayFactor(decayFactor)
  .setRandomCenters(numDimensions, 0.0)

//------------------------ functions --------------
      

def preformatForDouble(src : String) :String = {
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
    
def CreateDoubleArray(a: Array[Any], n: Int) = {
    val buffer: Array[Double] = Array.fill(n)(0.00)
    for( i <- 0 to n-1) {
        buffer(i) = a(i).toString.toDouble
    }
    buffer
}

def showRddStats(rdd: RDD[Vector], msgText : String) : Boolean = {
    //println(msgText + " check stats")
    try{
        val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)

//        println(summary.mean) // a dense vector containing the mean value for each column
//        println(summary.variance) // column-wise variance
//        println(summary.numNonzeros) // number of nonzeros in each column

        (summary.mean.size > 0)
    } catch {
        case e: IllegalArgumentException => { /*println(msgText + " Illegal Argument error: "); e.printStackTrace(); println(e.toString()); */ false }
        case e: IllegalStateException    => { /*println(msgText + " Illegal State error: "); e.printStackTrace(); println(e.toString());*/ false  }
        case e: IOException              => { /*println(msgText + " IO Exception error: "); e.printStackTrace(); println(e.toString());*/ false }
        case e: Throwable => { /*println(msgText + " Other error: "); e.printStackTrace(); println(e.toString());*/ false }
    }
}
      
def transformRddForModel(rdd : RDD[String], msgText : String) : RDD[Vector] = {
    val rdd1 : RDD[Vector] = rdd.map{ line =>
        { 
       //     println(msgText + " : line debug" + line.toString)
            JSON.parseFull(line)  match {
                case None => CreateEmptyArray()
                case Some( mapAsAny ) => mapAsAny match {
                    case x: Map[ String, Any ] => { CreateDataArray(x) }
                    case _ => CreateEmptyArray()
                }
            }
        }
    }
    .map{ x => {
         if (x.size>1)  CreateDoubleArray(x,4)
         else CreateDoubleArray(Array.fill(1)(0.00),1)
        }
    }
    .map(x => Vectors.dense(x))

    rdd1
}  

def getStreamData(srcStream : DStream[String], msgText : String) : DStream[Vector] = {
    try {
            srcStream.filter(!_.isEmpty)
            .transform{ rdd => transformRddForModel(rdd, msgText) }
            .transform{ rdd => if (showRddStats(rdd, msgText)) rdd else null }
            .filter(_!=null)
    } catch {
        case e: IllegalArgumentException => { /*println(msgText + " Illegal Argument error: "); e.printStackTrace(); print(e.toString());*/ null }
        case e: IllegalStateException    => { /*println(msgText + " Illegal State error: "); e.printStackTrace(); print(e.toString());*/ null }
        case e: IOException              => { /*println(msgText + " IO Exception error: "); e.printStackTrace(); print(e.toString());*/ null }
        case e: Throwable => { /*println(msgText + " Other error: "); e.printStackTrace(); print(e.toString());*/ null }
    }
}
    


//------------- start doing something ------------

//println("Input trades n ")
trades.count().print
ttrades.count().print
//messages.flatMap{case (_,line) => line}.foreach(a => println(a))
//trades.print()
//ttrades.print()

val nn = 3;
var msgText = "";
val partitionsEachInterval = 10
var numCollected = 0L
val fileName = "/shared/data/traindata" //  + time.milliseconds.toString
var textStream = ssc.textFileStream(fileName);

val props = new HashMap[String, Object]()
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
val producer = new KafkaProducer[String,String](props)

msgText = "generate train data"
println(msgText + " check point")
//var trainingData = getStreamData(trades, msgText)

try {
    var vectors =
        trades.filter(!_.isEmpty)
        .transform{ rdd =>
            rdd.map{ line =>
                {
                    JSON.parseFull(line)  match {
                        case None => CreateDoubleArray(Array.fill(1)(0.00),1)
                        case Some( mapAsAny ) => mapAsAny match {
                            case x: Map[ String, Any ] => { CreateDataArray(x) }
                            case _ => CreateDoubleArray(Array.fill(1)(0.00),1)
                        }
                    }
                }
            }
            .filter(_.size>1)
            .map{ x =>
                val n = 4
                var buffer: Array[Double] = Array.fill(n)(0.00)
                var line = ""
                for( i <- 0 to n-1) {
                    buffer(i) = x(i).toString.toDouble
                    if (i > 0) line += "|"
                    line += x(i).toString
                }

                buffer
            }
            .map(x => Vectors.dense(x))
        }
        .cache()

    vectors.count().print  // Calls an action to create the cache.
    //ssc.sparkContext.makeRDD(sModel.clusterCenters, numClusters).saveAsObjectFile("/model")

//    val model = KMeans.train(vectors, numClusters, numIterations)
//    model.clusterCenters

    vectors.foreachRDD{ (rdd,time) =>
        val count = rdd.count()
        if (count > 0) {
            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)

            println("------------Rdd stats (" + count + ") -------")

            println(summary.mean) // a dense vector containing the mean value for each column
            println(summary.variance) // column-wise variance
            println(summary.numNonzeros) // number of nonzeros in each column

                //val message = new ProducerRecord[String, String]("kmstats",null,"{value:"+summary.variance.toString+"}")
            val message = new KeyedMessage[String, String]("kmstats", time.toString, summary.mean.toString);
            producer.send(message)


//            val outputRDD = rdd.repartition(partitionsEachInterval)
//            outputRDD.saveAsTextFile(fileName/* + time.milliseconds.toString*/)

            numCollected += count
            if (numCollected > 10000) {
                System.exit(0)
            }
        }

    }

//    val inputData = textStream.map(Vectors.parse).cache()
//    println("------------Input data count -------")
//    inputData.count().print
//
//    sModel.trainOn(inputData)


//    var tvectors =  ttrades.filter(!_.isEmpty)
//        .transform{ rdd =>
//            rdd.map{ line =>
//                {
//                    JSON.parseFull(line)  match {
//                        case None => CreateDoubleArray(Array.fill(1)(0.00),1)
//                        case Some( mapAsAny ) => mapAsAny match {
//                            case x: Map[ String, Any ] => { CreateDataArray(x) }
//                            case _ => CreateDoubleArray(Array.fill(1)(0.00),1)
//                        }
//                    }
//                }
//            }
//            .filter(_.size>1)
//            .map{ x =>
//                val n = 4
//                val buffer: Array[Double] = Array.fill(n)(0.00)
//                for( i <- 0 to n-1) {
//                    buffer(i) = x(i).toString.toDouble
//                }
//                buffer
//            }
//            .map(x => Vectors.dense(x))
//        }
//        .transform(rdd => rdd.map{ x => ((x.toArray)(0), x) }).cache()
//
//    tvectors.count()  // Calls an action to create the cache.
//
//    sModel.predictOnValues(tvectors).print()
    //ssc.sparkContext().makeRDD(sModel.clusterCenters, numClusters).saveAsObjectFile("/model")

//        .transform { rdd =>
//            try{
//                val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)
//                rdd
//            } catch {
//                case e: Throwable => null
//            }
//        }
//        }
//    .foreachRDD{ (rdd,time) =>
//        val count = rdd.count()
//        if (count > 0) {
//            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)
//
//            println(summary.mean) // a dense vector containing the mean value for each column
//            println(summary.variance) // column-wise variance
//            println(summary.numNonzeros) // number of nonzeros in each column
//
//            val outputRDD = rdd.repartition(partitionsEachInterval)
//            outputRDD.saveAsTextFile("/traindata_" + time.milliseconds.toString)
//            numCollected += count
//            if (numCollected > 10000) {
//                System.exit(0)
//            }
//        }
    //}
//.foreachRDD((rdd, time) => {
//  val count = rdd.count()
//  if (count > 0) {
//    val outputRDD = rdd.repartition(partitionsEachInterval)
//    outputRDD.saveAsTextFile(
//      outputDirectory + "/tweets_" + time.milliseconds.toString)
//    numTweetsCollected += count
//    if (numTweetsCollected > numTweetsToCollect) {
//      System.exit(0)
//    }
//  }
//})
} catch {
    case e: Throwable => { println(msgText + " error: "); e.printStackTrace(); print(e.toString()); }
}

//msgText = "generate test data"
////val testingData = getStreamData(ttrades, msgText)
//println(msgText + " check point")
//try {
//    ttrades.filter(!_.isEmpty)
//    .transform{ rdd =>
//        rdd.map{ line =>
//            {
//                JSON.parseFull(line)  match {
//                    case None => CreateDoubleArray(Array.fill(1)(0.00),1)
//                    case Some( mapAsAny ) => mapAsAny match {
//                        case x: Map[ String, Any ] => { CreateDataArray(x) }
//                        case _ => CreateDoubleArray(Array.fill(1)(0.00),1)
//                    }
//                }
//            }
//        }
//        .filter(_.size>1)
//        .map{ x =>
//            val n = 4
//            val buffer: Array[Double] = Array.fill(n)(0.00)
//            for( i <- 0 to n-1) {
//                buffer(i) = x(i).toString.toDouble
//            }
//            buffer
//        }
//        .map(x => Vectors.dense(x))
//    }
//    .transform { rdd =>
//        try{
//            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)
//            rdd
//        } catch {
//            case e: Throwable => null
//        }
//    }
//    .filter(_!=null)
//    .transform(rdd => rdd.map{ x => ((x.toArray)(0), x) })
//    .print
//
////      }.foreachRDD{ rdd =>
////        val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)
////
////        println(summary.mean) // a dense vector containing the mean value for each column
////        println(summary.variance) // column-wise variance
////        println(summary.numNonzeros) // number of nonzeros in each column
////    }
//
//} catch {
//    case e: Throwable => { println(msgText + " error: "); e.printStackTrace(); print(e.toString()); }
//}
//
//msgText = "train data"
//println(msgText)
//try{
//      if (trainingData != null) {
//          trainingData.print
//            //sModel.trainOn(trainingData)
//      } else {
//          println("Null " + msgText)
//      }
//} catch {
//    case e: IllegalArgumentException => { println(msgText + " Illegal Argument error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IllegalStateException    => { println(msgText + " Illegal State error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IOException              => { println(msgText + " IO Exception error: "); e.printStackTrace(); println(e.toString()) }
//    case e: Throwable => { println(msgText + " Other error: "); e.printStackTrace(); println(e.toString()) }
//} finally {
//    println(msgText + " check point")
//}
//
//msgText = "predict on values"
//println(msgText)
//try {
//      if (testingData != null) {
//        testingData.print
////        sModel.predictOnValues(testingData.transform(rdd => rdd.map{ x => ((x.toArray)(0), x) })).print()
//      } else {
//           println("Null " + msgText)
//      }
//} catch {
//    case e: IllegalArgumentException => { println(msgText + " Illegal Argument error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IllegalStateException    => { println(msgText + " Illegal State error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IOException              => { println(msgText + " IO Exception error: "); e.printStackTrace(); println(e.toString()) }
//    case e: Throwable => { println(msgText + " Other error: "); e.printStackTrace(); println(e.toString()) }
//} finally {
//    println(msgText + " check point")
//}

//msgText = "predict"
//println(msgText)
//try{
//      if (trainingData != null) {
//        sModel.predictOn(trainingData).print()
//      } else {
//          println("Null " + msgText)
//      }
//} catch {
//    case e: IllegalArgumentException => { println(msgText + " Illegal Argument error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IllegalStateException    => { println(msgText + " Illegal State error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IOException              => { println(msgText + " IO Exception error: "); e.printStackTrace(); println(e.toString()) }
//    case e: Throwable => { println(msgText + " Other error: "); e.printStackTrace(); println(e.toString()) }
//} finally {
//    println(msgText + " check point")
//    if (trainingData != null) trainingData.print
//}

//msgText = "saving to parquet"
//println(msgText)
//try{
//    trades.foreachRDD{rdd =>
//        if (rdd.toLocalIterator.nonEmpty) {
//            val sqlContext = new SQLContext(rdd.sparkContext)
//            import sqlContext.implicits._
//
//            // Convert your data to a DataFrame, depends on the structure of your data
//            val df = sqlContext.jsonRDD(rdd).toDF
//            df.save("org.apache.spark.sql.parquet", SaveMode.Append, Map("path" -> path))
//        }
//    }
//} catch {
//    case e: IllegalArgumentException => { println(msgText + " Illegal Argument error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IllegalStateException    => { println(msgText + " Illegal State error: "); e.printStackTrace(); println(e.toString()) }
//    case e: IOException              => { println(msgText + " IO Exception error: "); e.printStackTrace(); println(e.toString()) }
//    case e: Throwable => { println(msgText + " Other error: "); e.printStackTrace(); println(e.toString()) }
//}
//
        ssc.start()
        ssc.awaitTermination()
    }
}
