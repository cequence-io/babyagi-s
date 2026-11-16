import Dependencies.Versions.*

name := "cohere-client-domain"

description := "Domain for Cohere API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient
)
