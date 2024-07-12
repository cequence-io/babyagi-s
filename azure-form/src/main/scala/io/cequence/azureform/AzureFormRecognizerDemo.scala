package io.cequence.azureform

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import io.cequence.azureform.model.AzureFormRecognizerApiVersion
import io.cequence.azureform.service.{AzureFormRecognizerHelper, AzureFormRecognizerServiceFactory}

import scala.concurrent.ExecutionContext.Implicits.global

object AzureFormRecognizerDemo extends AzureFormRecognizerHelper with App {

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = ActorMaterializer()

  private lazy val service = AzureFormRecognizerServiceFactory(
    System.getenv("CRUNCHER_AZURE_FORM_ENDPOINT"),
    System.getenv("CRUNCHER_AZURE_FORM_API_KEY")
  )

  private lazy val version = AzureFormRecognizerApiVersion.v2023_02_28_preview
  private val fileName = System.getenv("AZURE_TEST_FILE_NAME")

  {
    for {
      readResult <- service.analyzeLayout(
        file = new java.io.File(fileName),
        pages = Some("1-2"),
        apiVersion = version
      )
    } yield {
      val readResultSection = readResult.analyzeResult.getOrElse(
        throw new IllegalArgumentException("No analyze result")
      )

      println("API-Version: " + readResultSection.apiVersion)
      println("Pages #:     " + readResultSection.pages.size)

      service.close
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    println(ex.getMessage)
    service.close
    System.exit(1)
  }
}