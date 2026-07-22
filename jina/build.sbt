import Dependencies.Versions.*

name := "jina-client"

description := "Client and utils for Jina AI API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core-akka" % wsClient,
  "io.cequence" %% "ws-client-play-akka" % wsClient,

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
