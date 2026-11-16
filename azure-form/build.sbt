import Dependencies.Versions.*

name := "azure-form-recognizer"

description := "Client and utils for Azure Form Recognizer"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient,
  "io.cequence" %% "ws-client-jdk" % wsClient, // the default engine - any other ws-client engine on the classpath with a higher priority is picked instead
  "org.kynosarges" % "tektosyne" % "6.2.0", // Polygon/geometry calculation

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
