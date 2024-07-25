name := "azure-form-recognizer"

description := "Client and utils for Azure Form Recognizer"

val wsClientVersion = "0.5.4"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClientVersion,
  "io.cequence" %% "ws-client-play" % wsClientVersion,
  "org.kynosarges" % "tektosyne" % "6.2.0", // Polygon/geometry calculation

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
