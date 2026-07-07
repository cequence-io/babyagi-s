package io.cequence.mistral

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import io.cequence.mistral.model._
import io.cequence.mistral.service.{MistralOCRModel, MistralServiceFactory}
import io.cequence.wsclient.service.ws.Timeouts

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MistralAPIDemo extends App {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = ActorMaterializer()

  private val service = MistralServiceFactory(
    timeouts = Some(
      Timeouts(
        requestTimeout = Some(300000),
        readTimeout = Some(300000)
      )
    )
  )

  private val testPdfFileName = System.getenv("MISTRAL_TEST_FILE_NAME")

  private val pageCount = 14

  private def doAux(pageIntervals: Seq[(Int, Int)]) =
    service.uploadWithOCR(
      new java.io.File(testPdfFileName),
      pageIntervals = pageIntervals
    )

  {
    for {
      files <- service.listFiles(Some(0), Some(100))

      _ = println(s"Total files ${files.total}: ${files.data.map(_.filename).mkString(",")}")

      _ <- Future.sequence(
        files.data.map { fileInfo =>
          service.deleteFile(UUID.fromString(fileInfo.id))
        }
      )

      _ = println(s"Total files after delete ${files.total}: ${files.data.map(_.filename).mkString(",")}")

      ocrResponse1 <- doAux(Nil)

      ocrResponse2 <- doAux(Seq((0, pageCount / 2), (pageCount / 2 + 1, pageCount - 1)))

      ocrResponse3 <- doAux((0 to pageCount - 1).map(i => (i,i)))

      // OCR at scale (via the async Batch API) is 50% cheaper than the sync endpoint used above
      batchResults <- service.uploadWithOCRBatch(
        Seq(
          "doc-1" -> new java.io.File(testPdfFileName),
          "doc-2" -> new java.io.File(testPdfFileName)
        )
      )
    } yield {
      val content1 = ocrResponse1.pages.map(_.markdown).mkString("")
      val content2 = ocrResponse2.pages.map(_.markdown).mkString("")
      val content3 = ocrResponse3.pages.map(_.markdown).mkString("")

      println(s"Pages: ${ocrResponse1.pages.size} - length: ${content1.length}")
      println(s"Pages: ${ocrResponse2.pages.size} - length: ${content2.length}")
      println(s"Pages: ${ocrResponse3.pages.size} - length: ${content3.length}")

      batchResults.foreach { result =>
        result.ocrResponse match {
          case Some(ocrResponse) =>
            println(s"Batch item ${result.customId}: ${ocrResponse.pages.size} pages")
          case None =>
            println(s"Batch item ${result.customId} failed: ${result.errorMessage.getOrElse("unknown error")}")
        }
      }

      service.close()
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    println(ex.getMessage)
    service.close()
    System.exit(1)
  }
}
