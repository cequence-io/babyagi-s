import sbt.Keys.test

name := "babyagi-s-port"

description := "One-to-one port of Baby AGI in Scala"

val openAIScalaVersion = "1.0.0"
val pineconeScalaVersion = "0.1.2"

libraryDependencies ++= Seq(
  "io.cequence" %% "pinecone-scala-client" % pineconeScalaVersion,
  "io.cequence" %% "openai-scala-client" % openAIScalaVersion,
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
