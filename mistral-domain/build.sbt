import Dependencies.Versions.*

name := "mistral-client-domain"

description := "Domain for Mistral API"

libraryDependencies ++= Seq(
  "io.cequence" %% "ws-client-core" % wsClient
)
