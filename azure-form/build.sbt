name := "azure-form-recognizer"

description := "Client and utils for Azure Form Recognizer"

lazy val playWsVersion = settingKey[String]("Play WS version to use")

playWsVersion := {
  scalaVersion.value match {
    case "2.12.18" => "2.1.10"
    case "2.13.11" => "2.2.0-M3"
    case "3.2.2" => "2.2.0-M2" // Version "2.2.0-M3" was produced by an unstable release: Scala 3.3.0-RC3
    case _ => "2.1.10"
  }
}

libraryDependencies ++= Seq(
  "org.kynosarges" % "tektosyne" % "6.2.0",                                           // Polygon/geometry calculation
  "ai.x" %% "play-json-extensions" % "0.20.0",                                        // better JSON parsing

  "com.typesafe.play" %% "play-ahc-ws-standalone" % playWsVersion.value,
  "com.typesafe.play" %% "play-ws-standalone-json" % playWsVersion.value,

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
