package io.cequence.mistral.service

import akka.stream.Materializer
import io.cequence.mistral.JsonFormats._
import io.cequence.mistral.{MistralClientTimeoutException, MistralClientUnknownHostException}
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.WsRequestContext
import io.cequence.wsclient.service.WSClientEngine
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.ws.PlayWSClientEngine
import io.cequence.mistral.model.Document.DocumentURLChunk
import io.cequence.mistral.model._
import org.slf4j.LoggerFactory
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsObject, Json}

import java.io.File
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import java.nio.file.{Files, StandardOpenOption}

private class MistralServiceImpl(
  apiKey: String
)(
  implicit val ec: ExecutionContext,
  val materializer: Materializer
) extends MistralService
    with WSClientWithEngine {

  override protected type PEP = String
  override protected type PT = String

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override protected val engine: WSClientEngine = PlayWSClientEngine(
    coreUrl = "https://api.mistral.ai/v1/",
    requestContext = WsRequestContext(
      authHeaders = Seq(("Authorization", s"Bearer $apiKey"))
    ),
    recoverErrors = { (serviceEndPointName: String) =>
      {
        case e: TimeoutException =>
          throw new MistralClientTimeoutException(
            s"${serviceEndPointName} timed out: ${e.getMessage}."
          )
        case e: UnknownHostException =>
          throw new MistralClientUnknownHostException(
            s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
          )
      }
    }
  )

  object Endpoint {
    val ocr = "ocr"
    val files = "files"
  }

  override def ocr(
    document: Document,
    settings: OCRSettings
  ): Future[OCRResponse] = {
    val documentJson = Json.toJson(document)(documentFormat)

    execPOSTBody(
      Endpoint.ocr,
      body = Json.toJsObject(settings) ++ Json.obj("document" -> documentJson)
    ).map(
      _.asSafeJson[OCRResponse](ocrResponseFormat)
    )
  }

  override def ocrWithPages(
    document: Document,
    settings: OCRSettings,
    pageIntervals: Seq[(Int, Int)]
  ): Future[OCRResponse] =
    if (pageIntervals.isEmpty) {
      // No parallelism
      ocr(document, settings)
    } else {
      // We have pagination, so we need to process the document in chunks
      logger.debug(s"Processing OCR with page groups ${pageIntervals.mkString(", ")}")
          
      // Process each page group in parallel
      Future.sequence(
        pageIntervals.map { case (start, end) =>
          val pages = (start to end).toSeq

          logger.debug(s"Processing pages $start to $end")

          // TODO: retry    
          ocr(
            document,
            settings = settings.copy(pages = pages)
          )
        }
      ).map { responses =>
        // Combine all responses into a single OCRResponse
        responses.reduceOption { (acc, response) =>
          val accUsageInfo = acc.usageInfo
          val responseUsageInfo = response.usageInfo

          OCRResponse(
            pages = acc.pages ++ response.pages,
            model = acc.model,
            usageInfo = OCRUsageInfo(
              pagesProcessed = accUsageInfo.pagesProcessed + responseUsageInfo.pagesProcessed,
              docSizeBytes = Some(accUsageInfo.docSizeBytes.getOrElse(0) + responseUsageInfo.docSizeBytes.getOrElse(0))
            )
          )
        }.getOrElse(
          throw new IllegalStateException("At least one OCR response expected")
        )
      }
    }

  override def uploadFile(
    file: File,
    purpose: Option[String],
    fileName: Option[String]
  ): Future[FileUploadResponse] =
    execPOSTMultipart(
      Endpoint.files,
      fileParams = Seq(("file", file, fileName)),
      bodyParams = Seq("purpose" -> purpose)
    ).map(
      _.asSafeJson[FileUploadResponse](fileUploadResponseFormat)
    )

  override def uploadSource(
    source: Source[ByteString, _],
    purpose: Option[String],
    fileName: Option[String]
  ): Future[FileUploadResponse] = {
    val tempFile = Files.createTempFile(s"mistral-upload-${UUID.randomUUID()}", ".tmp")
    
    source
      .runWith(
        akka.stream.scaladsl.FileIO.toPath(
          tempFile,
          Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        )
      )
      .flatMap { _ =>
        val file = tempFile.toFile
        file.deleteOnExit() // Ensure the file is deleted when the JVM exits
        
        uploadFile(file, purpose, fileName).andThen {
          case _ => try {
            Files.deleteIfExists(tempFile) // Try to delete the file immediately after use
          } catch {
            case _: Throwable => // Ignore deletion errors
          }
        }
      }
  }

  override def signFileURL(
    fileId: UUID,
    expiryHours: Int
  ): Future[String] =
    execGET(
      endPoint = Endpoint.files,
      endPointParam = Some(fileId.toString + "/url"),
      params = Seq("expiry" -> Some(expiryHours))
    ).map { response =>
      val json = response.asSafeJson[JsObject]
      (json \ "url").as[String]
    }

  override def deleteFile(
    fileId: UUID
  ): Future[FileDeleteResponse] =
    execDELETE(
      endPoint = Endpoint.files,
      endPointParam = Some(fileId.toString)
    ).map(
      _.asSafeJson[FileDeleteResponse](fileDeleteResponseFormat)
    )

  override def listFiles(
    page: Option[Int],
    pageSize: Option[Int]
  ): Future[FileListResponse] =
    execGET(
      endPoint = Endpoint.files,
      params = Seq(
        "page" -> page,
        "page_size" -> pageSize
      )
    ).map(
      _.asSafeJson[FileListResponse](fileListResponseFormat)
    )

  override def uploadWithOCR(
    file: java.io.File,
    settings: OCRSettings,
    pageIntervals: Seq[(Int, Int)] = Nil,
    fileName: Option[String] = None
  ): Future[OCRResponse] =
    for {
      fileResponse <- uploadFile(
        file = file,
        purpose = Some("ocr"),
        fileName = fileName
      )

      _ = logger.debug(s"File ${fileResponse.filename} uploaded with id ${fileResponse.id}")

      ocrResponse <- ocrAux(
        fileResponse,
        settings,
        pageIntervals
      )
    } yield
      ocrResponse

  override def uploadSourceWithOCR(
    source: Source[ByteString, _],
    settings: OCRSettings = Defaults.OCR,
    pageIntervals: Seq[(Int, Int)] = Nil,
    fileName: Option[String] = None
  ): Future[OCRResponse] =
    for {
      fileResponse <- uploadSource(
        source = source,
        purpose = Some("ocr"),
        fileName = fileName
      )

      _ = logger.debug(s"File ${fileResponse.filename} uploaded with id ${fileResponse.id}")

      ocrResponse <- ocrAux(
        fileResponse,
        settings,
        pageIntervals
      )
    } yield
      ocrResponse

  private def ocrAux(
    fileResponse: FileUploadResponse,
    settings: OCRSettings,
    pageIntervals: Seq[(Int, Int)] = Nil
  ): Future[OCRResponse] = {
    val start = new java.util.Date().getTime

    for {
      signedURL <- signFileURL(
        fileResponse.id,
        expiryHours = 1
      )

      _ = logger.debug(s"${fileResponse.filename} signed with URL ${signedURL}")

      // Process OCR with pagination and parallelism if specified
      ocrResponse <- ocrWithPages(
        document = Document.DocumentURLChunk(
          documentUrl = signedURL,
          documentName = fileResponse.filename
        ),
        settings = settings,
        pageIntervals = pageIntervals
      )

      deleteResponse <- deleteFile(fileResponse.id)
    } yield {
      logger.info(
        s"OCR response with ${ocrResponse.pages.size} pages with ${pageIntervals.size} groups/splits for file ${fileResponse.filename} obtained in ${new java.util.Date().getTime - start} ms."
      )

      if (!deleteResponse.deleted)
        logger.warn(s"File ${fileResponse.filename} was not deleted from Mistral API.")

      ocrResponse
    }
  }
}

object MistralServiceFactory {

  private val envAPIKey = "MISTRAL_API_KEY"

  def apply(
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    apply(getAPIKeyFromEnv())

  def apply(
    apiKey: String
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    new MistralServiceImpl(apiKey)

  private def getAPIKeyFromEnv(): String =
    Option(System.getenv(envAPIKey)).getOrElse(
      throw new IllegalStateException(
        s"${envAPIKey} environment variable expected but not set. Alternatively, you can pass the API key explicitly to the factory method."
      )
    )
}
