package io.cequence.azureform.service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.model.{AzureInvoiceResponse, AzureLayoutResponse, AzureReadResponse, HasStatus}
import io.cequence.azureform.service.JsonUtil.JsonOps
import play.api.libs.json.{JsArray, JsNull, JsObject, Json}
import play.api.libs.ws.{BodyWritable, StandaloneWSRequest}
import io.cequence.azureform.service.AzureFormats._
import io.cequence.azureform.service.ws.{Timeouts, WSRequestHelper}
import org.slf4j.LoggerFactory
import play.api.libs.ws.DefaultBodyWritables

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
    apiVersion: String
  ): Future[String] = {
    val request = getWSRequestOptional(
      endPoint = Some(AzureFormRecognizerEndPoint.analyze),
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion)
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
    ).map(
      _.get("operation-location")
        .flatMap(
          _.headOption
            .map(url => url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?")))
        )
        .getOrElse(throw new Exception("Operation-Location header not found"))
    )
  }

  override def analyze(
    file: File,
    modelId: String,
    apiVersion: String
  ) = {
    val request = getWSRequestOptional(
      endPoint = Some(AzureFormRecognizerEndPoint.analyze),
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion)
      )
    )

    execRequestHeadersAux(
      request,
      _.post(file)(writableOf_File)
    ).map(
      _.get("operation-location")
        .flatMap(
          _.headOption
            .map(url => url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?")))
        )
        .getOrElse(throw new Exception("Operation-Location header not found"))
    )
  }

  override def analyzeSource(
    source: Source[ByteString, _],
    modelId: String,
    apiVersion: String
  ): Future[String] = {
    val request = getWSRequestOptional(
      endPoint = Some(AzureFormRecognizerEndPoint.analyze),
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion)
      )
    )

    execRequestHeadersAux(
      request,
      _.post(source)(writableOf_Source)
    ).map(
      _.get("operation-location")
        .flatMap(
          _.headOption
            .map(url => url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?")))
        )
        .getOrElse(throw new Exception("Operation-Location header not found"))
    )
  }

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
    apiVersion: String
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeReadResults
    )(
      file,
      modelId,
      apiVersion
    )

  override def analyzeLayout(
    file: File,
    modelId: String,
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeLayoutResults
    )(
      file,
      modelId,
      apiVersion
    )

  override def analyzeInvoice(
    file: File,
    modelId: String,
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyze,
      analyzeInvoiceResults
    )(
      file,
      modelId,
      apiVersion
    )

  override def analyzeReadSource(
    source: Source[ByteString, _],
    modelId: String,
    apiVersion: String
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeReadResults
    )(
      source,
      modelId,
      apiVersion
    )

  override def analyzeLayoutSource(
    source: Source[ByteString, _],
    modelId: String,
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeLayoutResults
    )(
      source,
      modelId,
      apiVersion
    )

  override def analyzeInvoiceSource(
    source: Source[ByteString, _],
    modelId: String,
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeSource,
      analyzeInvoiceResults
    )(
      source,
      modelId,
      apiVersion
    )

  override def analyzeReadRemote(
    urlSource: String,
    modelId: String,
    apiVersion: String
  ): Future[AzureReadResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeReadResults
    )(
      urlSource,
      modelId,
      apiVersion
    )

  override def analyzeLayoutRemote(
    urlSource: String,
    modelId: String,
    apiVersion: String
  ): Future[AzureLayoutResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeLayoutResults
    )(
      urlSource,
      modelId,
      apiVersion
    )

  override def analyzeInvoiceRemote(
    urlSource: String,
    modelId: String,
    apiVersion: String
  ): Future[AzureInvoiceResponse] =
    analyzeWithResultsAux(
      analyzeRemote,
      analyzeInvoiceResults
    )(
      urlSource,
      modelId,
      apiVersion
    )

  // aux/helper functions

  protected def analyzeWithResultsAux[T <: HasStatus, IN](
    analyzeFun: (IN, String, String) => Future[String],
    analyzeResultsFun: (String, String, String) => Future[T]
  )(
    input: IN,
    modelId: String,
    apiVersion: String
  ): Future[T] =
    for {
      resultId <- analyzeFun(input, modelId, apiVersion)

      result <- pollUntilDone(
        analyzeResultsFun(resultId, modelId, apiVersion)
      )
    } yield result

  private def execRequestHeadersAux(
    request: StandaloneWSRequest,
    exec: StandaloneWSRequest => Future[StandaloneWSRequest#Response]
  ): Future[Map[String, Seq[String]]] =
    exec(request).map(_.headers)

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
    params: Seq[(PT, Option[Any])] = Nil
  ): StandaloneWSRequest#Self =
    super.getWSRequestOptional(endPoint, endPointParam, params).addHttpHeaders(authHeader)

  override protected def getWSRequest(
    endPoint: Option[PEP],
    endPointParam: Option[String],
    params: Seq[(PT, Any)] = Nil
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