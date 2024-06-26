name := "azure-form-recognizer"

description := "Client and utils for Azure Form Recognizer"

lazy val playWsVersion = settingKey[String]("Play WS version to use")

def typesafePlayWS(version: String) = Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % version,
  "com.typesafe.play" %% "play-ws-standalone-json" % version
)

def orgPlayWS(version: String) = Seq(
  "org.playframework" %% "play-ahc-ws-standalone" % version,
  "org.playframework" %% "play-ws-standalone-json" % version
)

def playWsDependencies(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 12)) =>
      typesafePlayWS("2.1.10")

    case Some((2, 13)) =>
      typesafePlayWS("2.2.0-M3")

    case Some((3, 2)) =>
      typesafePlayWS("2.2.0-M2") // Version "2.2.0-M3" was produced by an unstable release: Scala 3.3.0-RC3

    case Some((3, 3)) =>
      orgPlayWS("3.0.0") // needs some work because of the akka -> pekko migration (https://pekko.apache.org/docs/pekko/current/project/migration-guides.html)

    // failover to the latest version
    case _ =>
      orgPlayWS("3.0.0")
  }

libraryDependencies ++= playWsDependencies(scalaVersion.value)

libraryDependencies ++= Seq(
  "org.kynosarges" % "tektosyne" % "6.2.0",                                           // Polygon/geometry calculation

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
