package io.cequence.mistral.service

import akka.actor.Scheduler
import akka.stream.Materializer
import io.cequence.mistral.JsonFormats._
import io.cequence.mistral.{MistralClientException, MistralClientNotFoundException, MistralClientTimeoutException, MistralClientUnknownHostException}
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.{
  CequenceWSTimeoutException,
  CequenceWSUnknownHostException,
  SiteBinding,
  WsRequestContext
}
import io.cequence.wsclient.service.{PollingHelper, WSClientEngine}
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.spi.{TransportSettings, WSClientEngineRegistry}
import io.cequence.wsclient.service.ws.Timeouts
import io.cequence.mistral.model.Document.DocumentURLChunk
import io.cequence.mistral.model._
import org.slf4j.LoggerFactory
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsNull, JsObject, Json}

import java.io.File
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import akka.stream.scaladsl.Source
import akka.util.ByteString

import java.nio.file.{Files, StandardOpenOption}

private class MistralServiceImpl(
  apiKey: String,
  timeouts: Option[Timeouts] = None,
  batchPollingIntervalMs: Int = 5000,
  externalEngine: Option[WSClientEngine] = None
)(
  implicit val ec: ExecutionContext,
  val materializer: Materializer
) extends MistralService
    with WSClientWithEngine
    with PollingHelper {

  override protected type PEP = String
  override protected type PT = String

  override protected val pollingMs = batchPollingIntervalMs

  // how many not-found (404) poll responses are tolerated for a freshly submitted batch job
  private val batchJobNotFoundMaxTolerated = 3

  // how many not-found (404) responses are tolerated on a GET/reference of a freshly created
  // file (the files API is eventually consistent, like the batch jobs API)
  private val fileNotFoundMaxTolerated = 3

  private implicit lazy val scheduler: Scheduler = materializer.system.scheduler

  protected val logger = LoggerFactory.getLogger(this.getClass)

  // classpath-discovered engine
  override protected val engine: WSClientEngine = externalEngine.getOrElse(
    WSClientEngineRegistry(TransportSettings(timeouts = timeouts.getOrElse(Timeouts())))
  )

  override protected def ownsEngine: Boolean = externalEngine.isEmpty

  override protected val site: SiteBinding = SiteBinding(
    coreUrl = "https://api.mistral.ai/v1/",
    requestContext = WsRequestContext(
      authHeaders = Seq(("Authorization", s"Bearer $apiKey"))
    ),
    recoverErrors = Some({ (serviceEndPointName: String) =>
      {
        case e @ (_: CequenceWSTimeoutException | _: TimeoutException) =>
          throw new MistralClientTimeoutException(
            s"${serviceEndPointName} timed out: ${e.getMessage}."
          )
        case e @ (_: CequenceWSUnknownHostException | _: UnknownHostException) =>
          throw new MistralClientUnknownHostException(
            s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
          )
      }
    })
  )

  override protected def handleErrorCodes(
    httpCode: Int,
    message: String
  ): Nothing = {
    val errorMessage = s"Code ${httpCode} : ${message}"
    httpCode match {
      case 404 => throw new MistralClientNotFoundException(errorMessage)
      case _   => throw new MistralClientException(errorMessage)
    }
  }

  object Endpoint {
    val ocr = "ocr"
    val files = "files"
    val batchJobs = "batch/jobs"
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
            ),
            documentAnnotation = acc.documentAnnotation.orElse(response.documentAnnotation)
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
      bodyParams = Seq("purpose" -> purpose),
      useInMemoryBody = true
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

  // Mistral's files API is eventually consistent: a freshly created file can be invisible for a
  // short while to a follow-up call - a GET, or a POST referencing its id - which then fails
  // with a 404 "File not found". A bounded number of not-founds is retried (spaced by the
  // polling delay) instead of failing - the same tolerance awaitBatchJob applies to fresh jobs.
  // Safe also around creating POSTs: a 404 response means nothing was created.
  private def withFileNotFoundTolerance[T](
    description: => String
  )(
    call: => Future[T]
  ): Future[T] = {
    def attempt(notFoundBudget: Int): Future[T] =
      call.recoverWith {
        case _: MistralClientNotFoundException if notFoundBudget > 0 =>
          logger.warn(
            s"File not found for ${description} - most likely not visible yet right after its creation. Retrying."
          )
          akka.pattern.after(pollingMs.millis, scheduler)(attempt(notFoundBudget - 1))
      }

    attempt(fileNotFoundMaxTolerated)
  }

  private def signFileURLTolerant(
    fileId: UUID,
    expiryHours: Int,
    filename: String
  ): Future[String] =
    withFileNotFoundTolerance(s"signing the URL of the file ${fileId} (${filename})")(
      signFileURL(fileId, expiryHours)
    )

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

  override def downloadFileContent(
    fileId: UUID
  ): Future[String] =
    execGET(
      endPoint = Endpoint.files,
      endPointParam = Some(fileId.toString + "/content")
    ).map(_.string)

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
      signedURL <- signFileURLTolerant(
        fileResponse.id,
        expiryHours = 1,
        filename = fileResponse.filename
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

  override def createBatchJob(
    endpoint: String,
    inputFileId: UUID,
    model: Option[String],
    metadata: Map[String, String],
    timeoutHours: Option[Int]
  ): Future[BatchJob] = {
    val request = BatchJobRequest(
      endpoint = endpoint,
      model = model,
      inputFiles = Some(Seq(inputFileId.toString)),
      metadata = if (metadata.isEmpty) None else Some(metadata),
      timeoutHours = timeoutHours
    )

    execPOSTBody(
      Endpoint.batchJobs,
      body = Json.toJsObject(request)(batchJobRequestFormat)
    ).map(
      _.asSafeJson[BatchJob](batchJobFormat)
    )
  }

  override def getBatchJob(
    jobId: String
  ): Future[BatchJob] =
    execGET(
      endPoint = Endpoint.batchJobs,
      endPointParam = Some(jobId)
    ).map(
      _.asSafeJson[BatchJob](batchJobFormat)
    )

  override def listBatchJobs(
    page: Option[Int],
    pageSize: Option[Int],
    createdByMe: Option[Boolean]
  ): Future[BatchJobList] =
    execGET(
      endPoint = Endpoint.batchJobs,
      params = Seq(
        "page" -> page,
        "page_size" -> pageSize,
        "created_by_me" -> createdByMe
      )
    ).map(
      _.asSafeJson[BatchJobList](batchJobListFormat)
    )

  override def cancelBatchJob(
    jobId: String
  ): Future[BatchJob] =
    execPOST(
      endPoint = Endpoint.batchJobs,
      endPointParam = Some(s"$jobId/cancel")
    ).map(
      _.asSafeJson[BatchJob](batchJobFormat)
    )

  override def awaitBatchJob(
    jobId: String
  ): Future[BatchJob] = {
    // Mistral's batch API is eventually consistent: a freshly created job can be invisible
    // to GET for a short while, so a bounded number of not-found responses is treated as
    // "still pending" rather than a failure
    val notFoundBudget = new AtomicInteger(batchJobNotFoundMaxTolerated)

    pollUntilDone[Option[BatchJob]](
      _.exists(job => BatchJobStatus.terminal.contains(job.status))
    )(
      getBatchJob(jobId).map(Some(_)).recover {
        case _: MistralClientNotFoundException if notFoundBudget.getAndDecrement() > 0 =>
          logger.warn(
            s"Batch job ${jobId} not found - most likely not visible yet right after its creation. Retrying."
          )
          None
      }
    ).map(
      _.getOrElse(
        throw new MistralClientException(
          s"Batch job ${jobId} polling completed without a job returned."
        )
      )
    )
  }

  override def ocrBatch(
    items: Seq[OCRBatchItem],
    settings: OCRSettings,
    metadata: Map[String, String],
    timeoutHours: Option[Int]
  ): Future[BatchJob] =
    if (items.isEmpty) {
      Future.failed(new MistralClientException("At least one OCR batch item expected."))
    } else {
      val jsonLines = items.map { item =>
        val itemSettings = item.settings.getOrElse(settings)
        val documentJson = Json.toJson(item.document)(documentFormat)
        val body = Json.toJsObject(itemSettings) ++ Json.obj("document" -> documentJson)

        Json.stringify(
          Json.obj(
            "custom_id" -> item.customId,
            "body" -> body
          )
        )
      }

      val jsonlContent = ByteString(jsonLines.mkString("\n"))

      for {
        fileResponse <- uploadSource(
          source = Source.single(jsonlContent),
          purpose = Some("batch"),
          fileName = Some(s"ocr-batch-${UUID.randomUUID()}.jsonl")
        )

        _ = logger.debug(
          s"OCR batch input with ${items.size} items uploaded as ${fileResponse.filename} (${fileResponse.id})."
        )

        batchJob <- withFileNotFoundTolerance(
          s"creating a batch job for the input file ${fileResponse.id} (${fileResponse.filename})"
        )(
          createBatchJob(
            endpoint = BatchEndpoint.ocr,
            inputFileId = fileResponse.id,
            model = Some(settings.model),
            metadata = metadata,
            timeoutHours = timeoutHours
          )
        )
      } yield batchJob
    }

  override def ocrBatchResults(
    job: BatchJob
  ): Future[Seq[OCRBatchItemResult]] =
    job.outputFile match {
      case None =>
        Future.failed(
          new MistralClientException(
            s"OCR batch job ${job.id} (status: ${job.status}) has no output file."
          )
        )

      case Some(outputFileId) =>
        withFileNotFoundTolerance(s"downloading the output file ${outputFileId} of the batch job ${job.id}")(
          downloadFileContent(UUID.fromString(outputFileId))
        ).map { content =>
          content
            .split("\n")
            .filter(_.trim.nonEmpty)
            .map { line =>
              val json = Json.parse(line)
              val customId = (json \ "custom_id").as[String]
              val errorJson = (json \ "error").toOption.filterNot(_ == JsNull)

              errorJson match {
                case Some(error) =>
                  OCRBatchItemResult(customId, errorMessage = Some(Json.stringify(error)))

                case None =>
                  val ocrResponse =
                    (json \ "response" \ "body").as[OCRResponse](ocrResponseFormat)
                  OCRBatchItemResult(customId, ocrResponse = Some(ocrResponse))
              }
            }
            .toSeq
        }
    }

  override def uploadWithOCRBatch(
    files: Seq[(String, File)],
    settings: OCRSettings,
    metadata: Map[String, String],
    signedUrlExpiryHours: Int
  ): Future[Seq[OCRBatchItemResult]] =
    Future
      .sequence(
        files.map { case (customId, file) =>
          uploadFile(file, purpose = Some("ocr"), fileName = None).map(customId -> _)
        }
      )
      .flatMap(
        uploadWithOCRBatchAux(_, settings, metadata, signedUrlExpiryHours)
      )

  override def uploadSourceWithOCRBatch(
    sources: Seq[(String, Source[ByteString, _])],
    settings: OCRSettings,
    metadata: Map[String, String],
    signedUrlExpiryHours: Int
  ): Future[Seq[OCRBatchItemResult]] =
    Future
      .sequence(
        sources.map { case (customId, source) =>
          uploadSource(source, purpose = Some("ocr"), fileName = None).map(customId -> _)
        }
      )
      .flatMap(
        uploadWithOCRBatchAux(_, settings, metadata, signedUrlExpiryHours)
      )

  // shared post-upload orchestration: sign URLs -> submit an OCR batch job -> await its
  // completion -> parse the results -> delete all the intermediate files
  private def uploadWithOCRBatchAux(
    fileResponses: Seq[(String, FileUploadResponse)],
    settings: OCRSettings,
    metadata: Map[String, String],
    signedUrlExpiryHours: Int
  ): Future[Seq[OCRBatchItemResult]] =
    for {
      items <- Future.sequence(
        fileResponses.map { case (customId, fileResponse) =>
          signFileURLTolerant(fileResponse.id, expiryHours = signedUrlExpiryHours, fileResponse.filename).map { signedURL =>
            OCRBatchItem(
              customId = customId,
              document = Document.DocumentURLChunk(
                documentUrl = signedURL,
                documentName = fileResponse.filename
              )
            )
          }
        }
      )

      submittedJob <- ocrBatch(items, settings, metadata)

      _ = logger.info(s"OCR batch job ${submittedJob.id} submitted with ${items.size} files.")

      finishedJob <- awaitBatchJob(submittedJob.id)

      results <-
        if (finishedJob.status == BatchJobStatus.Success)
          ocrBatchResults(finishedJob)
        else
          Future.failed(
            new MistralClientException(
              s"OCR batch job ${finishedJob.id} finished with status ${finishedJob.status}: " +
                finishedJob.errors.map(_.message).mkString("; ")
            )
          )

      cleanupFileIds =
        (fileResponses.map(_._2.id) ++
          finishedJob.inputFiles.map(UUID.fromString) ++
          finishedJob.outputFile.map(UUID.fromString) ++
          finishedJob.errorFile.map(UUID.fromString)).distinct

      _ <- Future.sequence(
        cleanupFileIds.map(id =>
          deleteFile(id).recover { case e: Throwable =>
            logger.warn(s"Failed to delete a batch-related file ${id}: ${e.getMessage}")
            FileDeleteResponse(id.toString, deleted = false)
          }
        )
      )
    } yield results
}

object MistralServiceFactory {

  private val envAPIKey = "MISTRAL_API_KEY"

  def apply(
    timeouts: Option[Timeouts] = None,
    batchPollingIntervalMs: Int = 5000
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    apply(getAPIKeyFromEnv(), timeouts, batchPollingIntervalMs)

  def apply(
    apiKey: String,
    timeouts: Option[Timeouts]
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    new MistralServiceImpl(apiKey, timeouts)

  def apply(
    apiKey: String,
    timeouts: Option[Timeouts],
    batchPollingIntervalMs: Int
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    new MistralServiceImpl(apiKey, timeouts, batchPollingIntervalMs)

  def withEngine(
    engine: WSClientEngine,
    apiKey: String,
    batchPollingIntervalMs: Int = 5000
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    new MistralServiceImpl(
      apiKey = apiKey,
      batchPollingIntervalMs = batchPollingIntervalMs,
      externalEngine = Some(engine)
    )

  /** Like `withEngine(engine, apiKey, ...)` but takes the API key from the MISTRAL_API_KEY env variable. */
  def withEngine(
    engine: WSClientEngine,
    batchPollingIntervalMs: Int
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): MistralService =
    withEngine(engine, getAPIKeyFromEnv(), batchPollingIntervalMs)

  private def getAPIKeyFromEnv(): String =
    Option(System.getenv(envAPIKey)).getOrElse(
      throw new IllegalStateException(
        s"${envAPIKey} environment variable expected but not set. Alternatively, you can pass the API key explicitly to the factory method."
      )
    )
}
