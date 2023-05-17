name := "babyagi-s-next"

description := "Next-gen Baby AGI in Scala"

val openAIScalaVersion = "0.3.2"
val pineconeScalaVersion = "0.1.0"

libraryDependencies ++= Seq(
  "io.cequence" %% "pinecone-scala-client" % pineconeScalaVersion,
  "io.cequence" %% "openai-scala-client" % openAIScalaVersion,

  // ONNX
  "com.microsoft.onnxruntime" % "onnxruntime" % "1.14.0",
  "ai.djl.huggingface" % "tokenizers" % "0.21.0",

  "com.typesafe.akka" %% "akka-http" % "10.2.10",

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime
)
