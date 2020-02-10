name := """collector"""
version := "1.0"
scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.6.3"
  val akkaHTTP    = "10.1.11"
  val logback     = "1.2.3"
  val redisClient = "3.20"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHTTP,
    "ch.qos.logback" % "logback-classic" % logback % Runtime,
    "net.debasishg" %% "redisclient" % redisClient
  )
}

fork in run := true
mainClass in (Compile, run) := Some("com.mgorokhovsky.collector.Collector")