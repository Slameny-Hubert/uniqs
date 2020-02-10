name := """service"""
version := "1.0"
scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.6.3"
  val akkaHTTP    = "10.1.11"
  val scalaTestV  = "3.1.0"
  val scalamock   = "4.4.0"
  val logback     = "1.2.3"
  val redisClient = "3.20"

  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHTTP,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHTTP,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHTTP % Test,
    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "org.scalamock" %% "scalamock" % scalamock % Test,
    "ch.qos.logback" % "logback-classic" % logback % Runtime,
    "net.debasishg" %% "redisclient" % redisClient
  )
}
fork in run := true
mainClass in (Compile, run) := Some("com.mgorokhovsky.service.HttpService")