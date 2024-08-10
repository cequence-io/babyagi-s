name := "azure-form-recognizer-domain"

description := "Domain for Azure Form Recognizer"

val wsClientVersion = "0.5.7"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClientVersion,
  "ai.x" %% "play-json-extensions" % "0.42.0" // available only for Scala 2.12 and 2.13, remove!
)
