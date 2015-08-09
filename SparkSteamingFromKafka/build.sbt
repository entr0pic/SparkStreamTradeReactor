assemblyJarName in assembly := "tradeStream.jar"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
 cp filter {x => x.data.getName.matches("sbt.*") || x.data.getName.matches(".*unused.*")}
}

resolvers ++= Seq(
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Typesafe" at  "http://dl.bintray.com/typesafe/maven-releases/akka-io-imports/1.0/",
    "Mapfish" at "http://dev.mapfish.org/maven/repository/"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.4.1" % "provided",
  "org.apache.spark" %% "spark-streaming" % "1.4.1" % "provided",
  "org.apache.spark" %% "spark-sql" % "1.4.1" % "provided" ,
  "com.typesafe.akka" % "akka-actor"   % "2.0.5" % "provided"  ,
  "com.typesafe.akka" % "akka-slf4j"   % "2.0.5" % "provided" ,
  "com.typesafe.akka" % "akka-remote"  % "2.0.5"  % "provided" ,
  "com.typesafe.akka" % "akka-agent"   % "2.0.5" % "provided" ,
  "com.typesafe.akka" % "akka-testkit" % "2.0.5" % "test",
    "voldemort.store.compress" % "h2-lzf" % "1.0"  % "provided" ,
  "org.apache.spark" %% "spark-streaming-kafka" % "1.4.1" % "provided",
    "org.apache.spark" %% "spark-mllib" % "1.4.1" % "provided" 
)
