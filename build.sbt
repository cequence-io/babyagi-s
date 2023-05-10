organization := "io.cequence"

name := "babyagi-s"

version := "0.0.1"

// Supported Scala versions
val scala212 = "2.12.15"
val scala213 = "2.13.10"
val scala3 = "3.2.2"

scalaVersion := scala212

val openAIScalaVersion = "0.3.2"
val pineconeScalaVersion = "0.0.1"

libraryDependencies ++= Seq(
  "io.cequence" %% "pinecone-scala-client" % pineconeScalaVersion,
  "io.cequence" %% "openai-scala-client" % openAIScalaVersion,
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)