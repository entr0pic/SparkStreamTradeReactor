import org.apache.spark.streaming.kafka._
import org.apache.spark._
import org.apache.spark.streaming._


object KafkaStream {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("trade-reader").setMaster("local[2]")
    val ssc = new StreamingContext(conf, Seconds(1))

    val kafkaStream = KafkaUtils.createStream(ssc, "docker:2181", "trade-generator", Map("trade-stream" -> 1))

    kafkaStream.print()

    ssc.start()
  }

}
