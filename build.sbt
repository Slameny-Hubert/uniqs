name := """uniqs"""
version := "1.0"
scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.6.3"
  val akkaHTTP    = "10.1.11"
  val scalaTestV  = "3.1.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHTTP,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHTTP,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHTTP % Test,
    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test
  )
}