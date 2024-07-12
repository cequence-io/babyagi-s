package io.cequence.azureform.service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.AzureFormats._
import io.cequence.azureform.JsonUtil.JsonOps
import io.cequence.azureform.model.{
  AzureInvoiceResponse,
  AzureLayoutResponse,
  AzureReadResponse,
  HasStatus
}
import io.cequence.wsclient.service.ws.{Timeouts, WSRequestHelper}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import play.api.libs.ws.{DefaultBodyWritables, StandaloneWSRequest}

import java.io.File
import scala.concurrent.{ExecutionContext, Future}

private class AzureFormRecognizerServiceImpl(
  endPoint: String,
  apiKey: String
)(
  implicit val ec: ExecutionContext,
  val materializer: Materializer,
  val actorSystem: ActorSystem
) extends AzureFormRecognizerService
    with WSRequestHelper
    with DefaultBodyWritables
    with AzureFormRecognizerHelper {

  override protected val coreUrl = s"${endPoint}/formrecognizer/documentModels/"
  override protected type PEP = AzureFormRecognizerEndPoint
  override protected type PT = AzureFormRecognizerParam

  override protected def timeouts: Timeouts = Timeouts()

  protected val logger = LoggerFactory.getLogger(this.getClass)

  private val authHeader = ("Ocp-Apim-Subscription-Key" -> apiKey)

  override def analyzeRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[String] = {
    val request = getWSRequestOptional(
      endPoint = Some(AzureFormRecognizerEndPoint.analyze),
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version.toString() -> Some(apiVersion),
        AzureFormRecognizerParam.pages -> pages
      )
    )

    val bodyParams = jsonBodyParams(
      AzureFormRecognizerParam.urlSource -> Some(urlSource)
    )

    val bodyParamsX = bodyParams.collect { case (fieldName, Some(jsValue)) =>
      (fieldName.toString, jsValue)
    }

    import play.api.libs.ws.JsonBodyWritables._

    execRequestHeadersAux(
      request,
      _.post(JsObject(bodyParamsX))
    ).map(getOperationLocation)
  }

  override def analyze(
    file: File,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ) = {
    val request = getWSRequestOptional(
      endPoint = Some(AzureFormRecognizerEndPoint.analyze),
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version.toString() -> Some(apiVersion),
        AzureFormRecognizerParam.pages -> pages
      )
    )

    execRequestHeadersAux(
      request,
      _.post(file)(writableOf_File)
    ).map(getOperationLocation)
  }

  override def analyzeSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[String] = {
    val request = getWSRequestOptional(
      endPoint = Some(AzureFormRecognizerEndPoint.analyze),
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version.toString() -> Some(apiVersion),
        AzureFormRecognizerParam.pages -> pages
      )
    )

    execRequestHeadersAux(
      request,
      _.post(source)(writableOf_Source)
    ).map(getOperationLocation)
  }

  private def getOperationLocation(headers: Map[String, Seq[String]]) =
    headers.get("operation-location").orElse(headers.get("Operation-Location"))
      .flatMap(
        _.headOption
          .map(url => url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?")))
      )
      .getOrElse(throw new Exception(s"Operation-Location header not found in ${headers.mkString(", ")}"))

  def analyzeReadResults(
    resultsId: String,
    modelId: String,
    apiVersion: String
  ): Future[AzureReadResponse] =
    execGET(
      AzureFormRecognizerEndPoint.analyzeResults,
      endPointParam = Some(modelId + "," + resultsId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion)
      )
    ).map(
      _.asSafe[AzureReadResponse]
    )

  def analyzeLayoutResults(
    resultsId: String,
    modelId: String,
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    execGET(
      AzureFormRecognizerEndPoint.analyzeResults,
      endPointParam = Some(modelId + "," + resultsId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion)
      )
    ).map(
      _.asSafe[AzureLayoutResponse]
    )

  def analyzeInvoiceResults(
    resultsId: String,
    modelId: String,
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    execGET(
      AzureFormRecognizerEndPoint.analyzeResults,
      endPointParam = Some(modelId + "," + resultsId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion)
      )
    ).map(
      _.asSafe[AzureInvoiceResponse]
    )

  override def analyzeRead(
    file: File,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeReadResults
    )(
      file,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeLayout(
    file: File,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeLayoutResults
    )(
      file,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeInvoice(
    file: File,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeInvoiceResults
    )(
      file,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeReadSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeReadResults
    )(
      source,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeLayoutSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeLayoutResults
    )(
      source,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeInvoiceSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeInvoiceResults
    )(
      source,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeReadRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeReadResults
    )(
      urlSource,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeLayoutRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeLayoutResults
    )(
      urlSource,
      modelId,
      pages,
      apiVersion
    )

  override def analyzeInvoiceRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeInvoiceResults
    )(
      urlSource,
      modelId,
      pages,
      apiVersion
    )

  // aux/helper functions

  protected def analyzeWithResultsAux[T <: HasStatus, IN](
    analyzeFun: (IN, String, Option[String], String) => Future[String],
    analyzeResultsFun: (String, String, String) => Future[T]
  )(
    input: IN,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[T] =
    for {
      resultId <- analyzeFun(input, modelId, pages, apiVersion)

      result <- pollUntilDone(
        analyzeResultsFun(resultId, modelId, apiVersion)
      )
    } yield result

  private def execRequestHeadersAux(
    request: StandaloneWSRequest,
    exec: StandaloneWSRequest => Future[StandaloneWSRequest#Response]
  ): Future[Map[String, Seq[String]]] =
    exec(request).map(_.headers.map(h => h._1 -> h._2.toSeq))

  override protected def createUrl(
    endpoint: Option[PEP],
    value: Option[String] = None // model id + results id
  ): String = {
    value
      .map(value =>
        if (value.contains(",")) {
          val parts = value.split(",").map(_.trim)
          val modelId = parts(0)
          val resultsId = parts(1)

          s"${coreUrl}${modelId}/${endpoint.map(_.toString).getOrElse("")}/${resultsId}"
        } else
          s"${coreUrl}${value}:${endpoint.map(_.toString).getOrElse("")}"
      )
      .getOrElse(
        throw new Exception("AzureFormRecognizerService: createUrl: endPointParam is None")
      )
  }

  override protected def getWSRequestOptional(
    endPoint: Option[PEP],
    endPointParam: Option[String],
    params: Seq[(String, Option[Any])] = Nil
  ): StandaloneWSRequest#Self =
    super.getWSRequestOptional(endPoint, endPointParam, params).addHttpHeaders(authHeader)

  override protected def getWSRequest(
    endPoint: Option[PEP],
    endPointParam: Option[String],
    params: Seq[(String, Any)] = Nil
  ): StandaloneWSRequest#Self =
    super.getWSRequest(endPoint, endPointParam, params).addHttpHeaders(authHeader)
}

object AzureFormRecognizerServiceFactory {
  def apply(
    endPoint: String,
    apiKey: String
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer,
    actorSystem: ActorSystem
  ): AzureFormRecognizerService = new AzureFormRecognizerServiceImpl(endPoint, apiKey)
}
