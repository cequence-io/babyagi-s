package io.cequence.azureform

import io.cequence.azureform.model.{
  AzureFormRecognizerApiVersion,
  AzureFormRecognizerAnalyzeSettings,
  ContentFormat
}
import io.cequence.azureform.service.{
  AzureFormRecognizerHelper,
  AzureFormRecognizerServiceFactory
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object AzureFormRecognizerDemo extends AzureFormRecognizerHelper with App {

  private val fileName = System.getenv("AZURE_TEST_FILE_NAME")

  private val versions = Seq(
//    AzureFormRecognizerApiVersion.v2022_08_31,
//    AzureFormRecognizerApiVersion.v2023_02_28_preview,
//    AzureFormRecognizerApiVersion.v2023_07_31,
//    AzureFormRecognizerApiVersion.v2023_10_31_preview,
//    AzureFormRecognizerApiVersion.v2024_02_29_preview,
//    AzureFormRecognizerApiVersion.v2024_07_31_preview,
    AzureFormRecognizerApiVersion.v2024_11_30
  )

  private val showContent = true

  private val settings = AzureFormRecognizerAnalyzeSettings(
//    pages = Some("1-2"),
//    features = Seq("languages"), // "styleFont",
    outputContentFormat = Some(ContentFormat.markdown)
  )

  // run for the given versions, one after another (and wait for it: a JDK-engine HTTP client and
  // the global EC run on daemon threads)
  private val run = versions
    .foldLeft(Future.successful(())) { (previous, version) =>
      previous.flatMap(_ => doAux(version).map(_ => ()))
    }
    .map { _ =>
      System.exit(0)
    }
    .recover { case ex: Throwable =>
      println(ex.getMessage)
      System.exit(1)
    }

  Await.ready(run, Duration.Inf)

  private def doAux(version: String) = {
    println("Running for the version: " + version)

    val service = AzureFormRecognizerServiceFactory(
      System.getenv("AZURE_FORM_RECOGNIZER_ENDPOINT"),
      System.getenv("AZURE_FORM_RECOGNIZER_API_KEY"),
      version
    )

    {
      for {
        readResult <- service.analyzeRead(
          file = new java.io.File(fileName),
          settings = settings
        )

        layoutResult <- service.analyzeLayout(
          file = new java.io.File(fileName),
          settings = settings
        )
      } yield {
        val readResultSection = readResult.analyzeResult.getOrElse(
          throw new IllegalArgumentException("No analyze result")
        )

        val layoutResultSection = layoutResult.analyzeResult.getOrElse(
          throw new IllegalArgumentException("No analyze result")
        )

        println("Read Result:")

        println(s"API-Version  : ${readResultSection.apiVersion}")
        println(s"Pages #      : ${readResultSection.pages.size}")
        println(s"Lines #      : ${readResultSection.pages.map(_.lines.size).sum}")
        println(s"Content size : ${readResultSection.content.length}")
        println(s"Languages    : ${readResultSection.languages.map(_.locale).mkString(", ")}")

        if (showContent)
          println(s"Content:\n${readResultSection.content}")

        println
        println("Layout Result:")

        println(s"API-Version     : ${layoutResultSection.apiVersion}")
        println(s"Pages #         : ${layoutResultSection.pages.size}")
        println(s"Tables #        : ${layoutResultSection.tables.size}")
        println(s"Lines #         : ${layoutResultSection.pages.map(_.lines.size).sum}")
        println(s"Content size    : ${layoutResultSection.content.length}")
        println(s"Content format  : ${layoutResultSection.contentFormat.getOrElse("N/A")}")
        println(
          s"Languages       : ${layoutResultSection.languages.map(_.locale).mkString(", ")}"
        )
        println(
          s"Paragraph Roles : ${layoutResultSection.paragraphs.flatMap(par => par.role).mkString(", ")}"
        )

        if (showContent)
          println(s"Content:\n${layoutResultSection.content}")

        if (showContent)
          println(s"Content (lines):\n${layoutResultSection.pages.map(_.lines.map(_.content).mkString("\n")).mkString("\n---PAGE BREAK---\n")}")

        println
        service.close()
      }
    } recover { case ex: Throwable =>
      service.close()
      throw ex
    }
  }
}
