cd SparkSteamingFromKafka
sbt package
sbt assembly
cd ..
cp -R SparkSteamingFromKafka/target/scala-2.10/. Spark/jars
