assemblyJarName in assembly := "tradeStream.jar"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
 cp filter {x => x.data.getName.matches("sbt.*") || x.data.getName.matches(".*unused.*")}
}

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.4.1" % "provided",
  "org.apache.spark" % "spark-streaming_2.10" % "1.4.1" % "provided",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.1"
)
