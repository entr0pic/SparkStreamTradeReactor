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
//import org.apache.kafka.serializer.StringDecoder
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.kafka.clients.producer.{ProducerConfig, KafkaProducer, ProducerRecord}
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

//--

//import org.apache.log4j.{Level, Logger}

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

//------------------------ base init --------------
    val Array(brokers, topics, path) = args
//    println(brokers)
//    println(topics)

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("TradeStreamReader")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    //val sqlContext = new SQLContext(sc)

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers, "auto.offset.reset" -> "smallest")
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics.split(",").filter(_=="trades").toSet)
    //val tmessages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics.split(",").filter(_=="ttrades").toSet)

    // create kafka message producer
    val props = new HashMap[String, Object]()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

    //props.put("client.id", "SparkHackathon-KafkaProducer")

    val producer = new KafkaProducer[String, String](props)

    // initialise streams from input messages
    val trades = messages.map(_._2)
    //val ttrades = tmessages.map(_._2)

//------------------------ functions --------------

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

/*
def transformRddForModel(rdd : RDD[String], msgText : String) : RDD[Vector] = {
    val rdd1 : RDD[Vector] = rdd.map{ line =>
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
    .map{ x => {
         if (x.size>1)  CreateDoubleArray(x,4)
         else CreateDoubleArray(Array.fill(1)(0.00),1)
        }
    }
    .map(x => Vectors.dense(x))

    rdd1
}
*/

//------------------------ init variables --------------

    // k-means related inits
    val numDimensions = 3
    val numClusters = 5
    val decayFactor = 1.0
    val numIterations = 100

// k-means streaming model - doesn't work
//    val sModel = new StreamingKMeans()
//      .setK(numClusters)
//      .setDecayFactor(decayFactor)
//      .setRandomCenters(numDimensions, 0.0)

    // k-means model
    val model = new KMeans()
          .setK(numClusters)
          .setMaxIterations(numIterations)

// processing related inits
var msgText = "";
val partitionsEachInterval = 10
var numCollected = 0L

// save to file inits - not used atm
//val dirName = "file:///shared/data/traindata/"
//val fileName =  dirName + "train"
//var textStream = ssc.textFileStream(dirName);

//------------- start doing something ------------

println("Input messages")
trades.count().print
//ttrades.count().print
//messages.flatMap{case (_,line) => line}.foreach(a => println(a))
//trades.print()
//ttrades.print()

try {
    /*
    val tvectors = ttrades.filter(!_.isEmpty)
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

    tvectors.count().print  // Calls an action to create the cache.
    */

    // transform messages to double vectors
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
        }.cache()

    vectors.count().print  // Calls an action to create the cache.

    // train k-means model & predict on random values
    vectors.foreachRDD{ (rdd,time) =>
        val count = rdd.count()
        if (count > 0) {
            var strMsg : String = ""
            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)

            println(s"------------Rdd stats (messages # in RDD = $count) -------")

            println(s"------------Rdd stats == mean value for each column == (messages # in RDD = $count) -------")
            println(summary.mean)
            println(s"------------Rdd stats == column-wise variance == (messages # in RDD = $count) -------")
            println(summary.variance)
            println(s"------------Rdd stats == number of nonzeros in each column == (messages # in RDD = $count) -------")
            println(summary.numNonzeros)

            strMsg += "[0],["+summary.mean.toArray.mkString(",")+"],["+summary.variance.toArray.mkString(",")+"]"

            val model2 = KMeans.train(rdd, numClusters, numIterations)

            println(s"------------Model cluster centers (clusters # $numClusters) -------")
            strMsg += ",[1]"
            model2.clusterCenters.foreach{ t =>
                println("["+t.toArray.mkString(",")+"]")
                strMsg += ",["+t.toArray.mkString(",")+"]"
//                val message2 = new ProducerRecord[String, String]("kmstats", null, "["+t.toArray.mkString(",")+"]");
//                val message2 = new ProducerRecord[String, String]("kmstats", null, t.toString);
//                producer.send(message2)
            }

            println(s"------------Model predict (clusters # $numClusters) -------")
            strMsg += ",[2]"
            val tdata = rdd.takeSample(true, 10, 1).foreach{ a =>
                val cluster = model2.predict(a) +1 // adding 1 for readability
                println(a)
                println(s"Predicted cluster = $cluster")
                strMsg += ",["+cluster+","+a.toArray.mkString(",")+"]"
//                    val message3 = new ProducerRecord[String, String]("kmstats", null, cluster.toString);
//                    producer.send(message3)
//                    val message4 = new ProducerRecord[String, String]("kmstats", null, a.toString);
//                    producer.send(message4)
            }

//            val message = new ProducerRecord[String, String]("kmstats", null, strMsg);
//            producer.send(message)
            println(s"------------Msg sent-------")
            println(summary.mean.toString)
            val message1 = new ProducerRecord[String, String]("kmstats", UUID.randomUUID().toString, summary.mean.toString);
            producer.send(message1)

            numCollected += count
            if (numCollected > 10000) {
                System.exit(0)
            }
        }

    }

//    vectors.repartition(partitionsEachInterval).saveAsTextFiles(fileName)
//
//    val inputData = textStream.map(Vectors.parse).cache()
//    println("------------Input data count -------")
//    inputData.count().print

//    sModel.trainOn(inputData)

} catch {
    case e: Throwable => { println(msgText + " error: "); e.printStackTrace(); print(e.toString()); }
}

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
