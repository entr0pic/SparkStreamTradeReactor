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
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
     val trades = messages.map(_._2)
      
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
      
def transformRddForModel(rdd : RDD[String], msg : String) : RDD[Array[Double]] = {
    var rdd1 = rdd.map{ line => 
        { 
            JSON.parseFull(line)  match {
                case None => CreateEmptyArray()
                case Some( mapAsAny ) => mapAsAny match {
                    case x: Map[ String, Any ] => { CreateDataArray(x) }
                    case _ => CreateEmptyArray()
                }
            }
        }
    }
    .filter(_.size==4)
    .map(x => CreateDoubleArray(x,4))

    println(msg)
    val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd1.map(Vectors.dense))

    println(summary.mean) // a dense vector containing the mean value for each column
    println(summary.variance) // column-wise variance
    println(summary.numNonzeros) // number of nonzeros in each column        

    rdd1
}  

//------------- start doing something ------------

trades.count().print
//    messages.flatMap(case (_,line) => line).print()

var nn = 10;

def getTrainData() : DStream[Array[Double]] = {

try {
    var i1 = 0;
    trades.filter(!_.isEmpty).map{ x => 
            if (i1 < nn) {
                i1+= 1
                x
            } else {
                i1 = 0
                null
            }
        }
        .filter(_ != null)
        .transform{ (rdd,t) => transformRddForModel(rdd, "training data check stats ("+i1+")") }
//        {
//            var rdd1 = rdd.map{ line => 
//                { 
//                    JSON.parseFull(line)  match {
//                        case None => CreateEmptyArray()
//                        case Some( mapAsAny ) => mapAsAny match {
//                            case x: Map[ String, Any ] => { CreateDataArray(x) }
//                            case _ => CreateEmptyArray()
//                        }
//                    }
//                }
//            }
//            .filter(_.size==4)
//            .map(x => CreateDoubleArray(x,4))
//            .map(Vectors.dense)
//            
//            println("training data check stats " + i1)
//            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd1)
//
//            println(summary.mean) // a dense vector containing the mean value for each column
//            println(summary.variance) // column-wise variance
//            println(summary.numNonzeros) // number of nonzeros in each column    
//                                                                              
//            rdd1
//        }
    
}catch {
    case e: IOException => {
        println("error: ")
        e.printStackTrace()
        print(e.toString())
    }
} finally {
    println("error generating training data") 
    null
}

}
    
var trainingData = getTrainData().cache()
    
      
def getTestData () : DStream[Array[Double]] = {
try{
    var i2 = 0
    trades.filter(!_.isEmpty).map{ x => 
            if (i2 == nn) {
                i2+= 1
                x
            } else {
                i2 = 0
                null
            }
        }
        .filter(_ != null)
        .transform{ (rdd,t) => transformRddForModel(rdd, "testing data check stats ("+i2+")") }
        
     
}catch {
    case e: IOException => {
        println("error: ")
        e.printStackTrace()
        print(e.toString())
    }
} finally {
    println("error generating testing data")   
    null
}
}
      
var testingData  = getTestData().map{ x => LabeledPoint(x(0), Vectors.dense(x)) }.cache()

try{
    println("train data")
    trainingData.print()
    sModel.trainOn(trainingData)
}catch {
    case e: IOException => {
        println("error: ")
        e.printStackTrace()
        print(e.toString())
    }
} finally {
    println("error on training data")   
}

//try {
//    println("predict on values")
//    testingData.print()
//    sModel.predictOnValues(testingData).print()
//} catch {
//    case e: IOException => {
//        println("error: ")
//        e.printStackTrace()
//        print(e.toString())
//    }
//} finally {
//    println("error predicting on testing data")   
//}
      
//try{
//    println("predict")
//    sModel.predictOn(trainingData).print()
//}catch {
//    case e: IOException => {
//        println("error: ")
//        e.printStackTrace()
//        print(e.toString())
//    }
//} finally {
//    println("error predicting training data")   
//}
 
      
//println(cleanData.size)
//val (left, right) = cleanData.splitAt(round(cleanData/size*0.9))
   
try{
    trades.foreachRDD{rdd =>
        if (rdd.toLocalIterator.nonEmpty) {
            val sqlContext = new SQLContext(rdd.sparkContext)
            import sqlContext.implicits._

            // Convert your data to a DataFrame, depends on the structure of your data
            val df = sqlContext.jsonRDD(rdd).toDF
            df.save("org.apache.spark.sql.parquet", SaveMode.Append, Map("path" -> path))
        }
    }
}catch {
    case e: IOException => {
        println("error: ")
        e.printStackTrace()
        print(e.toString())
    }
} finally {
    println("error saving to parquet")   
}

        ssc.start()
        ssc.awaitTermination()
    }
}
