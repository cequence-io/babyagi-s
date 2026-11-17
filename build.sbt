import sbt.Keys.test

// Supported versions
val scala212 = "2.12.18"
val scala213 = "2.13.18"
val scala32 = "3.2.2"

ThisBuild / organization := "io.cequence"
ThisBuild / scalaVersion := scala213
ThisBuild / version := "2.0.0"
ThisBuild / isSnapshot := false

lazy val port = (project in file("babyagi-s-port"))

lazy val next = (project in file("babyagi-s-next"))

// the published client modules cross-build (`sbt ++2.12.18 jina_client/test`, `+jina_client/publishSigned`)
val crossBuilt = Seq(crossScalaVersions := Seq(scala212, scala213))

lazy val azure_form_domain = (project in file("azure-form-domain")).settings(crossBuilt)

lazy val azure_form =
  (project in file("azure-form"))
    .settings(crossBuilt)
    .aggregate(azure_form_domain)
    .dependsOn(azure_form_domain)

lazy val cohere_client_domain = (project in file("cohere-domain")).settings(crossBuilt)

lazy val cohere_client =
  (project in file("cohere"))
    .settings(crossBuilt)
    .aggregate(cohere_client_domain, azure_form)
    .dependsOn(cohere_client_domain)

lazy val mistral_client_domain = (project in file("mistral-domain")).settings(crossBuilt)

lazy val mistral_client =
  (project in file("mistral"))
    .settings(crossBuilt)
    .aggregate(mistral_client_domain, cohere_client)
    .dependsOn(mistral_client_domain)

lazy val jina_client_domain = (project in file("jina-domain")).settings(crossBuilt)

lazy val jina_client =
  (project in file("jina"))
    .settings(crossBuilt)
    .aggregate(jina_client_domain, mistral_client)
    .dependsOn(jina_client_domain)

// unpublished engine matrix: the client modules carry no engine (a caller brings one), so the
// demos, the engine smoke checks and their logging config (matrix/shared) run here, on one
// ws-client engine each, e.g. `sbt "matrix_akka/runMain io.cequence.matrix.MistralEngineSmoke"`
def matrixProject(name: String, engineId: String, engineModules: Seq[ModuleID]) =
  Project(s"matrix_$name", file(s"matrix/$name"))
    .settings(
      crossBuilt,
      publish / skip := true,
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "matrix" / "shared" / "src" / "main" / "scala",
      Compile / unmanagedResourceDirectories += (ThisBuild / baseDirectory).value / "matrix" / "shared" / "src" / "main" / "resources",
      libraryDependencies ++= engineModules :+ ("ch.qos.logback" % "logback-classic" % "1.4.7" % Runtime),
      run / fork := true,
      run / javaOptions += s"-Dws-client.engine=$engineId"
    )
    .dependsOn(azure_form, cohere_client, mistral_client, jina_client)

lazy val matrix_jdk =
  matrixProject("jdk", "jdk", Seq("io.cequence" %% "ws-client-jdk" % Dependencies.Versions.wsClient))

lazy val matrix_akka =
  matrixProject("akka", "play-akka-stream", Seq("io.cequence" %% "ws-client-play-akka-stream" % Dependencies.Versions.wsClient))


// POM settings for Sonatype
ThisBuild / homepage := Some(url("https://github.com/cequence-io/babyagi-s"))

ThisBuild / sonatypeProfileName := "io.cequence"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/cequence-io/babyagi-s"),
    "scm:git@github.com:cequence-io/babyagi-s.git"
  )
)

ThisBuild / developers := List(
  Developer("bnd", "Peter Banda", "peter.banda@protonmail.com", url("https://peterbanda.net"))
)

ThisBuild / licenses += "MIT" -> url("https://opensource.org/licenses/MIT")

ThisBuild / publishMavenStyle := true

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

ThisBuild / publishTo := sonatypePublishToBundle.value
