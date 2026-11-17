import Dependencies.Versions.*

name := "jina-client"

description := "Client and utils for Jina AI API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient,
  "io.cequence" %% "ws-client-jdk" % wsClient % Test, // no engine at compile / runtime - a caller brings one (see the README)

  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)
