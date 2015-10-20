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
import java.util.UUID
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
      println(path)

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
    val buffer:Array[Any] = new Array[Any](5)
    buffer(0) = src("price")
    buffer(1) = src("party_weight")
    buffer(2) = src("exchange_weight")
    buffer(3) = src("currency_weight")
    buffer(4) = src("country_weight")
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

def getStringByWeight(a: Double) : String = {
    var ss = a.toString.substring(2)
    if (ss.length == 2) ss = "0" + ss
    if (ss.length == 1) ss = "00" + ss

    var buffer: String = ""
    if (ss.length == 3) {
        buffer = getUnicode(ss(0).toString, ss(1).toString, ss(2).toString)
    } else {
        for (i <- ss.length-1 to 0 by -3) {
            //buffer = ("""\""" +"u0"+(i>1?ss(i-2):"0")+(i>0?ss(i-1):"0")+ss(i)).toString + buffer

    //        val z = "0"
    //        var b = ss(i).toString
    //        if (i>0) b = ss(i-1).toString + b else b = z + b
    //        if (i>1) b = ss(i-2).toString + b else b = z + b
    //        b = """\""" +"u0" + b
    //        buffer = b + buffer
            val s1 = if (i > 1)  ss(i-2).toString else ""
                val s2 = if (i > 0 ) ss(i-1).toString else ""
              buffer = getUnicode(s1, s2, ss(i).toString) + buffer
        }
    }
    buffer
}

def getUnicode(s1:String, s2:String, s3:String) : String = {
//    val ret = """\""" +"u0" + s1 + s2 + s3
    Integer.parseInt(s1+s2+s3, 10).toChar.toString
}

def getLabelString(a: Array[Double], featsNum:Integer) : String = {
    var label : String = ""
    var labelBuf : Array[String] = Array.fill(featsNum)("")
    for (i <- 0 to featsNum-1) {
        if (i == 0) labelBuf(i) = "" // price has no weight, thus, no back ref label
        else labelBuf(i) = getStringByWeight(a(i).toDouble)
    }
    for (j <- 0 to featsNum-1)  {
        if (j > 0) label += ","
        label += '"'+labelBuf(j).toString+'"'
    }
    label
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
         if (x.size>1)  CreateDoubleArray(x,featuresNum)
         else CreateDoubleArray(Array.fill(1)(0.00),1)
        }
    }
    .map(x => Vectors.dense(x))

    rdd1
}
*/

//------------------------ init variables --------------

    // k-means related inits
    val numClusters = 5
    val decayFactor = 1.0
    val numIterations = 100

    val featuresNum = 4

// k-means streaming model - doesn't work
//      val numDimensions = 3
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
                val n = featuresNum
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
                val n = featuresNum
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

            strMsg += "{"

            strMsg += '"'+"time"+ '"'+":"+'"'+time.toString+'"'

            val summary: MultivariateStatisticalSummary = Statistics.colStats(rdd)

//            println(s"------------Rdd stats (messages # in RDD = $count) -------")
//            println(s"------------Rdd stats == mean value for each column == (messages # in RDD = $count) -------")
//            println(summary.mean)
//            println(s"------------Rdd stats == column-wise variance == (messages # in RDD = $count) -------")
//            println(summary.variance)
//            println(s"------------Rdd stats == number of nonzeros in each column == (messages # in RDD = $count) -------")
//            println(summary.numNonzeros)

            strMsg += ","+s"${'"'}rdd-stats${'"'}"+":["
            strMsg += summary.mean.toString+","+summary.variance.toString
            strMsg += "]"

            val model2 = KMeans.train(rdd, numClusters, numIterations)

            println(s"------------Model cluster centers (clusters # $numClusters) -------")

            strMsg += ","+s"${'"'}cluster-centers${'"'}"+":["

            var firstCluster : Boolean = true;
            var clusterLabels : Array[String] = Array.fill(numClusters)("")
            var k = 0
            model2.clusterCenters.foreach{ t =>
                println(t.toString)
                if (firstCluster) {
                    firstCluster = false
                } else {
                    strMsg += ","
                    k+=1
                }
                strMsg +=  t.toString

                // fill the labels for centers
                clusterLabels(k) = ""
                for(i <- 0 to featuresNum-1) {
                    if (i>0) clusterLabels(k) += ","
                    clusterLabels(k) += '"'+'"'
                }

//                clusterLabels(k) = getLabelString(t.toArray, featuresNum)
//                var labelBuf : Array[String] = Array.fill(featuresNum)("")
//                for (i <- 0 to featuresNum-1) {
//                    if (i == 0) labelBuf(i) = "" // price has no weight, thus, no back ref label
//                    else labelBuf(i) = getStringByWeight(t(i).toDouble)
//                }
//                labels(k) = ""
//                for (j <- 0 to featuresNum-1)  {
//                    if (j > 0) labels(k) += ","
//                    labels(k) += '"'+labelBuf(j).toString+'"'
//                }
            }
            strMsg += "]"

            strMsg += ","+s"${'"'}cluster-centers-labels${'"'}"+":["
            var printMsg = ""
            for( i <- 0 to numClusters-1) {
                if (i>0) {
                    strMsg += ","
                    printMsg += "," // debug
                }
                strMsg += "[" + clusterLabels(i) + "]"
                printMsg += (i+1)+":"+"[" + clusterLabels(i) + "]" // debug
            }

            strMsg += "]"

//            println(s"------------Model centers labels  -------")
//            println(printMsg)

            var sampleSize = (count/2).toInt
            if (sampleSize > 130) sampleSize = 130;
            else {
                if (sampleSize < 70) sampleSize = (count*0.75).toInt
            }

            println(s"------------Model predict (clusters # $numClusters), sample size : $sampleSize -------")

            val buffer: Array[String] = Array.fill(numClusters)("")
            var nums : Array[Integer] = Array.fill(numClusters)(0)
            var labels : Array[String] = Array.fill(numClusters)("")
            val maxNum : Integer = 20;
            val tdata = rdd.takeSample(true, sampleSize, 1).foreach{ a =>
                val cluster = model2.predict(a)  // adding 1 for readability
//                println(a)
//                println("Predicted cluster = "+ (1+cluster).toString)
                if (buffer(cluster) != "") {
                    buffer(cluster) += ","
                }
                if (labels(cluster) != "") {
                    labels(cluster) += ","
                }

                if (nums(cluster) <= maxNum){ // limit each cluster to <=20 examples
                    buffer(cluster) += a.toString
                    labels(cluster) += "["+ getLabelString(a.toArray, featuresNum)+"]"
                }
                nums(cluster) += 1
            }

            strMsg += ","+s"${'"'}cluster-nums${'"'}"+":["

            printMsg = ""
            for( i <- 0 to numClusters-1) {
                if (i>0) {
                    strMsg += ","
                    printMsg += ","
                }
                strMsg += nums(i).toString
                printMsg += (i+1)+":"+nums(i).toString
            }

            strMsg += "]"

            println(s"------------Model predict (size of predicted clusters)  -------")
            println(printMsg)


            strMsg += ","+s"${'"'}cluster-data${'"'}"+":["

            var firstRec : Boolean = true;
            buffer.foreach{s =>
                if (s != "") {
                    if (firstRec) {
                        firstRec = false
                    } else {
                        strMsg += ","
                    }
                    strMsg += "["+s+"]"
                }
            }
            strMsg += "]"

            strMsg += ","+s"${'"'}cluster-labels${'"'}"+":["

            var firstLbl : Boolean = true;
            printMsg = ""
            labels.foreach{s =>
                if (s != "") {
                    if (firstLbl) {
                        firstLbl = false
                    } else {
                        strMsg += ","
                        printMsg += "," // debug
                    }
                    strMsg += "["+s+"]"
                    printMsg += "["+s+"]" //debug
                }
            }
            strMsg += "]"

//            println(s"------------ Back ref labels  -------")
//            println(printMsg)

            strMsg += "}"

//            println(s"------------Message to send-------")
//            println(strMsg)

            val message = new ProducerRecord[String, String]("kmstats", null, strMsg);
            producer.send(message)
//            println(s"------------Msg sent-------")


//            numCollected += count
//            if (numCollected > 10000) {
//                System.exit(0)
//            }
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
//    val dirNameStatic = "file:///shared/data/"
//    var bankRdd = ssc.sparkContext.textFile(dirNameStatic+"banks.json")
//    if (bankRdd.toLocalIterator.nonEmpty) {
//        val sqlContext = new SQLContext(bankRdd.sparkContext)
//        import sqlContext.implicits._
//
//        // Convert your data to a DataFrame, depends on the structure of your data
//        val df = sqlContext.jsonRDD(bankRdd).toDF
//        df.registerTempTable("banks")
//        val banks = sqlContext.sql("SELECT * FROM banks")
//        println(banks.take(10))
////        df.save("org.apache.spark.sql.parquet", SaveMode.Append, Map("path" -> path))
//    }
//} catch {
////    case e: IllegalArgumentException => { println(msgText + " Illegal Argument error: "); e.printStackTrace(); println(e.toString()) }
////    case e: IllegalStateException    => { println(msgText + " Illegal State error: "); e.printStackTrace(); println(e.toString()) }
////    case e: IOException              => { println(msgText + " IO Exception error: "); e.printStackTrace(); println(e.toString()) }
//    case e: Throwable => { println(msgText + " Other error: "); e.printStackTrace(); println(e.toString()) }
//}

        ssc.start()
        ssc.awaitTermination()
    }
}
