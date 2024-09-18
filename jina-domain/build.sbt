import Dependencies.Versions.*

name := "jina-client-domain"

description := "Domain for Jina AI API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient
)
