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
    
      def CreateDoubleArray(a: Array[Any], n: Int) = {
          val buffer: Array[Double] = Array.fill(n)(0.00)
          for( i <- 0 to n-1) {
              buffer(i) = a(i).toString.toDouble
          }
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
      
      var i = 0

val numDimensions = 3
val numClusters = 2
val decayFactor = 1.0
      
val sModel = new StreamingKMeans()
  .setK(numClusters)
  .setDecayFactor(decayFactor)
  .setRandomCenters(numDimensions, 0.0)
      
//    messages.flatMap(case (_,line) => line).print()

//              var  testingData = cleanData.map(_.take(4)).filter(_.size==4).map{ x => 
//                  LabeledPoint(x(0).toString.toDouble, Vectors.dense(x.map(_.toString.toDouble)))
//            }
//    model.predictOnValues(testingData).print()
//
  
def transformRddForModel(rdd : RDD[Vector[Any]], i: Int, msg : String) : RDD[Vector[Double]] = {
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

    println(msg + i)
    val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd1.map(Vectors.dense))

    println(summary.mean) // a dense vector containing the mean value for each column
    println(summary.variance) // column-wise variance
    println(summary.numNonzeros) // number of nonzeros in each column        

    rdd1
}  

def transformTrainingRdd(rdd: RDD[Vector[Any]], i: Int) : RDD[Vector[Double]] = {
    transformRddForModel(rdd, i, "training data check stats ")
}
      
def transformTestingRdd(rdd: RDD[Vector[Any]], i: Int) : RDD[Vector[Double]] = {
    transformRddForModel(rdd, i, "testing data check stats ")
}
      
 try {
      
      var i1 = 0;
      var nn = 10;
      
      var trainingData = trades
        .filter(!_.isEmpty)
        .map{ x => 
            if (i1 < nn) {
                i1+= 1
                x
            } else {
                i1 = 0
                null
            }
        }
        .filter(_ != null)
        .transform{(rdd,t) => transformTrainingRdd(rdd, i1)
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
        }
    }.cache()
    
    var i2 = 0
    var testingData = trades
        .filter(!_.isEmpty)
        .map{ x => 
            if (i2 == nn) {
                i2+= 1
                x
            } else {
                i2 = 0
                null
            }
        }
        .filter(_ != null)
        .transform{ (rdd,t) => transformTestingRdd(rdd, i2)
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
//        
//            println("testing data check stats " + i2)
//            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd1.map(Vectors.dense))
//
//            println(summary.mean) // a dense vector containing the mean value for each column
//            println(summary.variance) // column-wise variance
//            println(summary.numNonzeros) // number of nonzeros in each column        
//            
//            rdd1
        }
    }
      
     testingData = testingData.map{ x => LabeledPoint(x(0), Vectors.dense(x)) }.cache()
     
      trainingData.print()
      testingData.print()
      
//    println("training data check stats")
//    trainingData.foreachRDD{ (rdd, _) => {
//            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)
//
//            println(summary.mean) // a dense vector containing the mean value for each column
//            println(summary.variance) // column-wise variance
//            println(summary.numNonzeros) // number of nonzeros in each column        
//        }
//    }
      

//      //val random = new XORShiftRandom(seed)
//    val clusterCenters = Array.fill(numClusters)(Vectors.dense(Array.fill(numDimensions)(0.00)))
//      
//    val weights = Array.fill(numClusters)(0.10)
//      
//      var model: StreamingKMeansModel = new StreamingKMeansModel(clusterCenters, weights)
      
     println("train data")
     sModel.trainOn(trainingData)
     println("predict on values")
    sModel.predictOnValues(testingData).print()
     println("predict")
     sModel.predictOn(trainingData).print()
  }catch {
  case e: IOException => {
      println("error: ")
    e.printStackTrace()
    print(e.toString())
  }
} finally {
    println("error on training data")   
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
    //trades.count().print
      
    ssc.start()
    ssc.awaitTermination()
  }
}
