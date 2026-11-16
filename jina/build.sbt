import Dependencies.Versions.*

name := "jina-client"

description := "Client and utils for Jina AI API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient,
  "io.cequence" %% "ws-client-jdk" % wsClient, // the default engine - any other ws-client engine on the classpath with a higher priority is picked instead

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
