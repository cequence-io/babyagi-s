package io.cequence.azureform

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import io.cequence.azureform.service.{AzureFormRecognizerHelper, AzureFormRecognizerServiceFactory}

import scala.concurrent.ExecutionContext.Implicits.global

object AzureFormRecognizerDemo extends AzureFormRecognizerHelper with App {

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = ActorMaterializer()

  private lazy val service = AzureFormRecognizerServiceFactory.apply(
    System.getenv("CRUNCHER_AZURE_FORM_ENDPOINT"),
    System.getenv("CRUNCHER_AZURE_FORM_API_KEY")
  )

  private lazy val version = "2023-07-31" // AzureFormRecognizerApiVersion.v2022_08_31
  private val fileName = "/home/peter/Data/contractool/Orange/Kreativny_Institut/Kreatívny_inštitút_Trenčín_RZ.pdf"
//  private val fileName = "/home/peter/Data/contractool/Lexikon_VZ/29Af82_2020_104.pdf"

  {
    for {
      readResult <- service.analyzeRead(
        file = new java.io.File(fileName),
        apiVersion = version
      )
    } yield {
      val readResultSection = readResult.analyzeResult.getOrElse(
        throw new IllegalArgumentException("No analyze result")
      )

      println(readResultSection.apiVersion)

      service.close
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    println(ex.getMessage)
    service.close
    System.exit(1)
  }
}