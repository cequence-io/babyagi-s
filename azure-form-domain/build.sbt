name := "azure-form-recognizer-domain"

description := "Domain for Azure Form Recognizer"

lazy val playJsonVersion = settingKey[String]("Play JSON version to use")

inThisBuild(
  playJsonVersion := {
    scalaVersion.value match {
      case "2.12.18" => "2.8.2"
      case "2.13.11" => "2.10.0-RC7"
      case "3.2.2"   => "2.10.0-RC6"
      case _         => "2.8.2"
    }
  }
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % playJsonVersion.value,
  "ai.x" %% "play-json-extensions" % "0.42.0" // available only for Scala 2.12 and 2.13, remove!
)
