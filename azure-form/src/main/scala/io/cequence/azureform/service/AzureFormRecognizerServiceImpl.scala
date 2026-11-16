package io.cequence.azureform.service

import io.cequence.azureform.{AzureFormRecognizerClientException, AzureFormRecognizerClientTimeoutException, AzureFormRecognizerClientUnknownHostException}
import io.cequence.azureform.AzureFormats._
import io.cequence.azureform.model.{AzureFormRecognizerAnalyzeSettings, AzureInvoiceResponse, AzureLayoutResponse, AzureReadResponse, HasStatus}
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.{CequenceWSTimeoutException, CequenceWSUnknownHostException, RichResponse, SiteBinding, WsRequestContext}
import io.cequence.wsclient.service.WSClientEngine
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.spi.{TransportSettings, WSClientEngineRegistry}
import io.cequence.wsclient.stream.{AsyncStream, Bytes}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import java.io.File
import java.net.UnknownHostException
import java.nio.channels.FileChannel
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private class AzureFormRecognizerServiceImpl(
  endPoint: String,
  apiKey: String,
  apiVersion: String,
  externalEngine: Option[WSClientEngine] = None
)(
  implicit val ec: ExecutionContext
) extends AzureFormRecognizerService
    with WSClientWithEngine
    with AzureFormRecognizerHelper {

  override protected type PEP = AzureFormRecognizerEndPoint
  override protected type PT = AzureFormRecognizerParam

  protected val logger = LoggerFactory.getLogger(this.getClass)

  // classpath-discovered engine (jdk unless an engine with a higher priority is on the classpath)
  override protected val engine: WSClientEngine =
    externalEngine.getOrElse(WSClientEngineRegistry(TransportSettings()))

  override protected def ownsEngine: Boolean = externalEngine.isEmpty

  override protected val site: SiteBinding = SiteBinding(
    coreUrl = endPoint,
    requestContext = WsRequestContext(
      authHeaders = Seq(
        "Ocp-Apim-Subscription-Key" -> apiKey
      ),
      extraParams = Seq(
        AzureFormRecognizerParam.overload
          .toString() -> "analyzeDocument", // needed for >= v4.0
        AzureFormRecognizerParam.api_version.toString() -> apiVersion
      )
    ),
    recoverErrors = Some((serviceEndPointName: String) => {
      case e @ (_: CequenceWSTimeoutException | _: TimeoutException) =>
        throw new AzureFormRecognizerClientTimeoutException(
          s"${serviceEndPointName} timed out: ${e.getMessage}."
        )
      case e @ (_: CequenceWSUnknownHostException | _: UnknownHostException) =>
        throw new AzureFormRecognizerClientUnknownHostException(
          s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
        )
    })
  )

  override def analyzeRemote(
    urlSource: String,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[String] =
    execPOSTRich(
      AzureFormRecognizerEndPoint.analyze,
      endPointParam = endPointParamAux(modelId),
      params = paramsAux(settings),
      bodyParams = jsonBodyParams(
        params = AzureFormRecognizerParam.urlSource -> Some(urlSource)
      )
    ).map(getOperationLocation)

  override def analyze(
    file: File,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[String] =
    execPOSTFileRich(
      AzureFormRecognizerEndPoint.analyze,
      endPointParam = endPointParamAux(modelId),
      urlParams = paramsAux(settings),
      file = file
    ).map(getOperationLocation)

  // the stream is written to a temp file and sent like analyze(file) - any engine, the same request
  override def analyzeSource(
    source: AsyncStream[Bytes],
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[String] =
    writeToTempFile(source).flatMap { tempFile =>
      analyze(tempFile.toFile, modelId, settings).andThen { case _ =>
        Try(Files.deleteIfExists(tempFile))
      }
    }

  private def writeToTempFile(source: AsyncStream[Bytes]): Future[Path] =
    Future {
      val tempFile = Files.createTempFile("azure-form-upload-", ".tmp")
      (tempFile, FileChannel.open(tempFile, StandardOpenOption.WRITE))
    }.flatMap { case (tempFile, channel) =>
      source
        .runForeach { bytes =>
          val buffer = bytes.asByteBuffer
          while (buffer.hasRemaining) channel.write(buffer)
        }
        .transform { result =>
          Try(channel.close())
          result match {
            case Success(_) =>
              Success(tempFile)

            case Failure(e) =>
              Try(Files.deleteIfExists(tempFile))
              Failure(e)
          }
        }
    }

  // Azure's api-version-dependent URL scheme (AzureFormRecognizerURL) - the engines get the whole URL as the site's
  // core URL with no endpoint (a 1.x engine subclass, AzurePlayWSClientEngine, rewrote it inside Play WS)

  private def azureSite(
    endPoint: PEP,
    endPointParam: Option[String]
  ): SiteBinding =
    site.copy(coreUrl = AzureFormRecognizerURL(site.coreUrl, endPoint.toString, endPointParam))

  override def execGETRich(
    endPoint: PEP,
    endPointParam: Option[String],
    params: Seq[(PT, Option[Any])],
    extraHeaders: Seq[(String, String)],
    acceptableStatusCodes: Seq[Int]
  ): Future[RichResponse] =
    inFuture(azureSite(endPoint, endPointParam)) { azure =>
      engine.execGETRich(
        azure,
        endPoint = "",
        endPointParam = None,
        paramTuplesToStrings(params),
        extraHeaders,
        acceptableStatusCodes
      )
    }

  override def execPOSTRich(
    endPoint: PEP,
    endPointParam: Option[String],
    params: Seq[(PT, Option[Any])],
    bodyParams: Seq[(PT, Option[JsValue])],
    extraHeaders: Seq[(String, String)],
    acceptableStatusCodes: Seq[Int]
  ): Future[RichResponse] =
    inFuture(azureSite(endPoint, endPointParam)) { azure =>
      engine.execPOSTRich(
        azure,
        endPoint = "",
        endPointParam = None,
        paramTuplesToStrings(params),
        paramTuplesToStrings(bodyParams),
        extraHeaders,
        acceptableStatusCodes
      )
    }

  override def execPOSTFileRich(
    endPoint: PEP,
    endPointParam: Option[String],
    urlParams: Seq[(PT, Option[Any])],
    file: File,
    extraHeaders: Seq[(String, String)],
    acceptableStatusCodes: Seq[Int]
  ): Future[RichResponse] =
    inFuture(azureSite(endPoint, endPointParam)) { azure =>
      engine.execPOSTFileRich(
        azure,
        endPoint = "",
        endPointParam = None,
        paramTuplesToStrings(urlParams),
        file,
        extraHeaders,
        acceptableStatusCodes
      )
    }

  // a malformed endpoint param fails the call's future, never the caller's thread
  private def inFuture[T](
    azure: => SiteBinding
  )(
    call: SiteBinding => Future[T]
  ): Future[T] =
    Try(azure) match {
      case Success(binding) =>
        call(binding)

      case Failure(e) =>
        Future.failed(e)
    }

  private def getOperationLocation(response: RichResponse) = {
    val headers = response.headers

    headers
      .get("operation-location")
      .orElse(headers.get("Operation-Location"))
      .flatMap(
        _.headOption.map(url => url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?")))
      )
      .getOrElse(
        throw new AzureFormRecognizerClientException(
          s"Operation-Location header not found in ${headers
              .mkString(", ")}. Status: ${response.status.code} - ${response.status.message}"
        )
      )
  }

  def analyzeReadResults(
    resultsId: String,
    modelId: String
  ): Future[AzureReadResponse] =
    execGET(
      AzureFormRecognizerEndPoint.analyzeResults,
      endPointParam = endPointParamAux(modelId, Some(resultsId))
    ).map(
      _.asSafeJson[AzureReadResponse]
    )

  def analyzeLayoutResults(
    resultsId: String,
    modelId: String
  ): Future[AzureLayoutResponse] =
    execGET(
      AzureFormRecognizerEndPoint.analyzeResults,
      endPointParam = endPointParamAux(modelId, Some(resultsId))
    ).map(
      _.asSafeJson[AzureLayoutResponse]
    )

  def analyzeInvoiceResults(
    resultsId: String,
    modelId: String
  ): Future[AzureInvoiceResponse] =
    execGET(
      AzureFormRecognizerEndPoint.analyzeResults,
      endPointParam = endPointParamAux(modelId, Some(resultsId))
    ).map(
      _.asSafeJson[AzureInvoiceResponse]
    )

  private def endPointParamAux(
    modelId: String,
    resultsId: Option[String] = None
  ) = resultsId match {
    case Some(id) => Some(apiVersion + "," + modelId + "," + id)
    case None     => Some(apiVersion + "," + modelId)
  }

  private def paramsAux(settings: AzureFormRecognizerAnalyzeSettings) =
    Seq(
      AzureFormRecognizerParam.pages -> settings.pages,
      AzureFormRecognizerParam.outputContentFormat -> settings.outputContentFormat.map(
        _.toString
      )
    ) ++ settings.features.map(feat => AzureFormRecognizerParam.features -> Some(feat))

  override def analyzeRead(
    file: File,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeReadResults
    )(
      file,
      modelId,
      settings
    )

  override def analyzeLayout(
    file: File,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeLayoutResults
    )(
      file,
      modelId,
      settings
    )

  override def analyzeInvoice(
    file: File,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeInvoiceResults
    )(
      file,
      modelId,
      settings
    )

  override def analyzeReadSource(
    source: AsyncStream[Bytes],
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeReadResults
    )(
      source,
      modelId,
      settings
    )

  override def analyzeLayoutSource(
    source: AsyncStream[Bytes],
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeLayoutResults
    )(
      source,
      modelId,
      settings
    )

  override def analyzeInvoiceSource(
    source: AsyncStream[Bytes],
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeInvoiceResults
    )(
      source,
      modelId,
      settings
    )

  override def analyzeReadRemote(
    urlSource: String,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeReadResults
    )(
      urlSource,
      modelId,
      settings
    )

  override def analyzeLayoutRemote(
    urlSource: String,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeLayoutResults
    )(
      urlSource,
      modelId,
      settings
    )

  override def analyzeInvoiceRemote(
    urlSource: String,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeInvoiceResults
    )(
      urlSource,
      modelId,
      settings
    )

  // aux/helper functions

  private def analyzeWithResultsAux[T <: HasStatus, IN](
    analyzeFun: (IN, String, AzureFormRecognizerAnalyzeSettings) => Future[String],
    analyzeResultsFun: (String, String) => Future[T]
  )(
    input: IN,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings
  ): Future[T] =
    for {
      resultId <- analyzeFun(input, modelId, settings)

      result <-
        pollUntilDone[T](
          _.status != "running"
        )(
          analyzeResultsFun(resultId, modelId)
        )
    } yield result
}

object AzureFormRecognizerServiceFactory extends AzureFormRecognizerConsts {
  def apply(
    endPoint: String,
    apiKey: String,
    apiVersion: String = Defaults.version
  )(
    implicit ec: ExecutionContext
  ): AzureFormRecognizerService =
    new AzureFormRecognizerServiceImpl(endPoint, apiKey, apiVersion)

  /**
   * Creates a service running on the given (shared, externally-owned) engine - no private HTTP
   * transport is created. Any ws-client engine works (the service builds Azure's
   * api-version-dependent URLs itself), and one engine can be shared by any number of
   * services / endpoints. The caller owns the engine and closes it - `close()` on the returned
   * service is a no-op transport-wise.
   */
  def withEngine(
    engine: WSClientEngine,
    endPoint: String,
    apiKey: String,
    apiVersion: String = Defaults.version
  )(
    implicit ec: ExecutionContext
  ): AzureFormRecognizerService =
    new AzureFormRecognizerServiceImpl(endPoint, apiKey, apiVersion, Some(engine))
}
