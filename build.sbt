import sbt.Keys.test

// Supported versions
val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala32 = "3.2.2"

ThisBuild / organization := "io.cequence"
ThisBuild / scalaVersion := scala212
ThisBuild / version := "0.1.18"
ThisBuild / isSnapshot := false

lazy val port = (project in file("babyagi-s-port"))

lazy val next = (project in file("babyagi-s-next"))

lazy val azure_form_domain = (project in file("azure-form-domain"))

lazy val azure_form =
  (project in file("azure-form"))
    .aggregate(azure_form_domain)
    .dependsOn(azure_form_domain)

lazy val cohere_client_domain = (project in file("cohere-domain"))

lazy val cohere_client =
  (project in file("cohere"))
    .aggregate(cohere_client_domain, azure_form)
    .dependsOn(cohere_client_domain)

lazy val mistral_client_domain = (project in file("mistral-domain"))

lazy val mistral_client =
  (project in file("mistral"))
    .aggregate(mistral_client_domain, cohere_client)
    .dependsOn(mistral_client_domain)

lazy val jina_client_domain = (project in file("jina-domain"))

lazy val jina_client =
  (project in file("jina"))
    .aggregate(jina_client_domain, mistral_client)
    .dependsOn(jina_client_domain)


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
