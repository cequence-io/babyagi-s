name := "azure-form-recognizer-domain"

description := "Domain for Azure Form Recognizer"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.2",
  "ai.x" %% "play-json-extensions" % "0.42.0" // available only for Scala 2.12 and 2.13
)
