package io.cequence.matrix

import io.cequence.mistral.model.{Document, OCRSettings}
import io.cequence.mistral.service.{MistralOCRModel, MistralServiceFactory}
import io.cequence.wsclient.service.spi.{TransportSettings, WSClientEngineRegistry}
import io.cequence.wsclient.service.ws.Timeouts
import io.cequence.wsclient.stream.{AsyncStream, Bytes}

import java.io.File
import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

// A non-destructive check of the Mistral client on the engine picked from the classpath
// (unlike MistralAPIDemo it never deletes files it has not uploaded itself).
// Expects MISTRAL_API_KEY and MISTRAL_TEST_FILE_NAME (a PDF with at least two pages); the OCR
// model can be overridden with MISTRAL_OCR_MODEL.
object MistralEngineSmoke extends App {

  private val engine = WSClientEngineRegistry(TransportSettings())
  println(s"Engine: ${engine.getClass.getName}")
  engine.close()

  // a sync OCR call can take longer than the default 60s request timeout
  private val service = MistralServiceFactory(
    timeouts = Some(Timeouts(requestTimeout = Some(300000), readTimeout = Some(300000)))
  )

  private val file = new File(System.getenv("MISTRAL_TEST_FILE_NAME"))

  // several chunks to exercise a streamed upload
  private def fileStream: AsyncStream[Bytes] = {
    val bytes = Files.readAllBytes(file.toPath)
    AsyncStream.fromIterator(() => bytes.grouped(64 * 1024).map(Bytes(_)))
  }

  private val model = sys.env.getOrElse("MISTRAL_OCR_MODEL", MistralOCRModel.mistral_ocr_4_1)

  private val twoPages = OCRSettings(model, pages = Seq(0, 1))

  // the JDK engine and the global EC run on daemon threads - wait for the run
  private val run = {
    for {
      files <- service.listFiles(Some(0), Some(5))
      _ = println(s"listFiles: ${files.total} files in total")

      fromFile <- service.uploadWithOCR(file, twoPages, pageIntervals = Seq((0, 0), (1, 1)))
      _ = println(s"uploadWithOCR (2 page groups): ${fromFile.pages.size} pages, model ${fromFile.model}")

      fromSource <- service.uploadSourceWithOCR(fileStream, twoPages, fileName = Some(file.getName))
      _ = println(s"uploadSourceWithOCR: ${fromSource.pages.size} pages")

      allParams <- service.ocr(
        Document.DocumentURLChunk("https://arxiv.org/pdf/1706.03762", "attention.pdf"),
        OCRSettings(
          model = model,
          pages = Seq(0),
          includeImageBase64 = Some(false),
          imageLimit = Some(1),
          imageMinSize = Some(10),
          tableFormat = Some("html"),
          extractHeader = Some(true),
          extractFooter = Some(true),
          includeBlocks = Some(true),
          confidenceScoresGranularity = Some("block")
        )
      )
      _ = println(
        s"ocr (all params): ${allParams.pages.size} page, blocks: ${allParams.pages.flatMap(_.blocks.toSeq.flatten).size}"
      )

      batch <- service.uploadWithOCRBatch(
        Seq("doc-1" -> file),
        OCRSettings(model, pages = Seq(0))
      )
      _ = batch.foreach { result =>
        println(
          s"batch ${result.customId}: ${result.ocrResponse.map(_.pages.size + " page(s)").getOrElse(result.errorMessage.getOrElse("?"))}"
        )
      }
    } yield {
      require(fromFile.pages.size == 2 && fromSource.pages.size == 2 && allParams.pages.size == 1)
      require(batch.forall(_.ocrResponse.isDefined), "a batch item failed")
      println("OK")
      service.close()
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    ex.printStackTrace()
    service.close()
    System.exit(1)
  }

  Await.ready(run, Duration.Inf)
}
