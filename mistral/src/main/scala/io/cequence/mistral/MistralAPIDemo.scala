package io.cequence.mistral

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import io.cequence.mistral.model._
import io.cequence.mistral.service.{MistralOCRModel, MistralServiceFactory}

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global

object MistralAPIDemo extends App {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = ActorMaterializer()

  private val service = MistralServiceFactory()

  private val testPdfFileName = System.getenv("MISTRAL_TEST_FILE_NAME")

  {
    for {
      ocrResponse <- service.uploadWithOCR(
        new File(new java.io.File(testPdfFileName)),
        settings = OCRSettings(
          model = MistralOCRModel.mistral_ocr_2503,
          includeImageBase64 = Some(false)
        )
      )
    } yield {
      println(ocrResponse.pages.map(_.markdown).mkString("\n---PAGE BREAK---\n"))
      service.close()
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    println(ex.getMessage)
    service.close()
    System.exit(1)
  }
}