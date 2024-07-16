package io.cequence.azureform.service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.AzureFormats._
import io.cequence.azureform.model.{
  AzureInvoiceResponse,
  AzureLayoutResponse,
  AzureReadResponse,
  HasStatus
}
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.WsRequestContext
import io.cequence.wsclient.service.WSClientEngine
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.ws.{AzurePlayWSClientEngine}
import org.slf4j.LoggerFactory
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
    with WSClientWithEngine
    with DefaultBodyWritables
    with AzureFormRecognizerHelper {

  override protected type PEP = AzureFormRecognizerEndPoint
  override protected type PT = AzureFormRecognizerParam

  protected val logger = LoggerFactory.getLogger(this.getClass)

  private val authHeader = ("Ocp-Apim-Subscription-Key" -> apiKey)

  override protected val engine: WSClientEngine = AzurePlayWSClientEngine(
    endPoint,
    requestContext = WsRequestContext(
      authHeaders = Seq(authHeader)
    )
  )

  override def analyzeRemote(
    urlSource: String,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[String] = {
    val bodyParams = jsonBodyParams(
      Seq(AzureFormRecognizerParam.urlSource -> Some(urlSource)): _*
    )

    execPOSTRich(
      AzureFormRecognizerEndPoint.analyze,
      endPointParam = Some(modelId),
      params = Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion),
        AzureFormRecognizerParam.pages -> pages
      ),
      bodyParams = bodyParams
    ).map { reponse =>
      getOperationLocation(reponse.headers)
    }
  }

  override def analyze(
    file: File,
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[String] =
    execPOSTFileRich(
      AzureFormRecognizerEndPoint.analyze,
      Some(modelId),
      Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion),
        AzureFormRecognizerParam.pages -> pages
      ),
      file = file
    ).map { reponse =>
      getOperationLocation(reponse.headers)
    }

  override def analyzeSource(
    source: Source[ByteString, _],
    modelId: String,
    pages: Option[String],
    apiVersion: String
  ): Future[String] =
    execPOSTSourceRich(
      AzureFormRecognizerEndPoint.analyze,
      Some(modelId),
      Seq(
        AzureFormRecognizerParam.api_version -> Some(apiVersion),
        AzureFormRecognizerParam.pages -> pages
      ),
      source
    ).map { reponse =>
      getOperationLocation(reponse.headers)
    }

  private def getOperationLocation(headers: Map[String, Seq[String]]) =
    headers
      .get("operation-location")
      .orElse(headers.get("Operation-Location"))
      .flatMap(
        _.headOption.map(url => url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?")))
      )
      .getOrElse(
        throw new Exception(
          s"Operation-Location header not found in ${headers.mkString(", ")}"
        )
      )

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
      _.asSafeJson[AzureReadResponse]
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
      _.asSafeJson[AzureLayoutResponse]
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
      _.asSafeJson[AzureInvoiceResponse]
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
