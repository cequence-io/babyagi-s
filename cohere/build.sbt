import Dependencies.Versions.*

name := "cohere-client"

description := "Client and utils for Cohere API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient,
  "io.cequence" %% "ws-client-play" % wsClient,

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
