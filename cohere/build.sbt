import Dependencies.Versions.*

name := "cohere-client"

description := "Client and utils for Cohere API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient,
  "io.cequence" %% "ws-client-jdk" % wsClient % Test, // no engine at compile / runtime - a caller brings one (see the README)

  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)
