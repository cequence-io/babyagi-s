package io.cequence.azureform.service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.AzureFormRecognizerClientException
import io.cequence.azureform.AzureFormats._
import io.cequence.azureform.model.{
  AzureInvoiceResponse,
  AzureLayoutResponse,
  AzureReadResponse,
  HasStatus
}
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.{RichResponse, WsRequestContext}
import io.cequence.wsclient.service.WSClientEngine
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.ws.AzurePlayWSClientEngine
import org.slf4j.LoggerFactory

import java.io.File
import scala.concurrent.{ExecutionContext, Future}

private class AzureFormRecognizerServiceImpl(
  endPoint: String,
  apiKey: String,
  apiVersion: String
)(
  implicit val ec: ExecutionContext,
  val materializer: Materializer,
  val actorSystem: ActorSystem
) extends AzureFormRecognizerService
    with WSClientWithEngine
    with AzureFormRecognizerHelper {

  override protected type PEP = AzureFormRecognizerEndPoint
  override protected type PT = AzureFormRecognizerParam

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override protected val engine: WSClientEngine = AzurePlayWSClientEngine(
    endPoint,
    requestContext = WsRequestContext(
      authHeaders = Seq(
        "Ocp-Apim-Subscription-Key" -> apiKey
      ),
      extraParams = Seq(
        AzureFormRecognizerParam.overload
          .toString() -> "analyzeDocument", // needed for >= v4.0
        AzureFormRecognizerParam.api_version.toString() -> apiVersion
      )
    )
  )

  override def analyzeRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[String] =
    execPOSTRich(
      AzureFormRecognizerEndPoint.analyze,
      endPointParam = endPointParamAux(modelId),
      params = paramsAux(pages, features),
      bodyParams = jsonBodyParams(
        params = AzureFormRecognizerParam.urlSource -> Some(urlSource)
      )
    ).map(getOperationLocation)

  override def analyze(
    file: File,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[String] =
    execPOSTFileRich(
      AzureFormRecognizerEndPoint.analyze,
      endPointParam = endPointParamAux(modelId),
      urlParams = paramsAux(pages, features),
      file = file
    ).map(getOperationLocation)

  override def analyzeSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[String] =
    execPOSTSourceRich(
      AzureFormRecognizerEndPoint.analyze,
      endPointParam = endPointParamAux(modelId),
      urlParams = paramsAux(pages, features),
      source
    ).map(getOperationLocation)

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

  private def paramsAux(
    pages: Option[String],
    features: Seq[String]
  ) =
    Seq(
      AzureFormRecognizerParam.pages -> pages
    ) ++ features.map(feat => AzureFormRecognizerParam.features -> Some(feat))

  override def analyzeRead(
    file: File,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeReadResults
    )(
      file,
      modelId,
      pages,
      features
    )

  override def analyzeLayout(
    file: File,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeLayoutResults
    )(
      file,
      modelId,
      pages,
      features
    )

  override def analyzeInvoice(
    file: File,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeInvoiceResults
    )(
      file,
      modelId,
      pages,
      features
    )

  override def analyzeReadSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeReadResults
    )(
      source,
      modelId,
      pages,
      features
    )

  override def analyzeLayoutSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeLayoutResults
    )(
      source,
      modelId,
      pages,
      features
    )

  override def analyzeInvoiceSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeInvoiceResults
    )(
      source,
      modelId,
      pages,
      features
    )

  override def analyzeReadRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeReadResults
    )(
      urlSource,
      modelId,
      pages,
      features
    )

  override def analyzeLayoutRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeLayoutResults
    )(
      urlSource,
      modelId,
      pages,
      features
    )

  override def analyzeInvoiceRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeInvoiceResults
    )(
      urlSource,
      modelId,
      pages,
      features
    )

  // aux/helper functions

  private def analyzeWithResultsAux[T <: HasStatus, IN](
    analyzeFun: (IN, String, Option[String], Seq[String]) => Future[String],
    analyzeResultsFun: (String, String) => Future[T]
  )(
    input: IN,
    modelId: String,
    pages: Option[String],
    features: Seq[String]
  ): Future[T] =
    for {
      resultId <- analyzeFun(input, modelId, pages, features)

      result <- pollUntilDone(
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
    implicit ec: ExecutionContext,
    materializer: Materializer,
    actorSystem: ActorSystem
  ): AzureFormRecognizerService =
    new AzureFormRecognizerServiceImpl(endPoint, apiKey, apiVersion)
}
