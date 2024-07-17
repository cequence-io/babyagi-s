package io.cequence.wsclient.service.ws
import akka.stream.Materializer
import io.cequence.azureform.model.AzureFormRecognizerApiVersion
import io.cequence.azureform.{
  AzureFormRecognizerClientException,
  AzureFormRecognizerClientTimeoutException,
  AzureFormRecognizerClientUnknownHostException
}
import io.cequence.wsclient.domain.{
  RichResponse,
  WsRequestContext
}
import io.cequence.wsclient.service.WSClientEngine

import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import scala.concurrent.ExecutionContext

class AzurePlayWSClientEngine(
  override val coreUrl: String,
  override protected val requestContext: WsRequestContext,
  override protected val recoverErrors: String => PartialFunction[Throwable, RichResponse]
)(
  override protected implicit val materializer: Materializer,
  override protected implicit val ec: ExecutionContext
) extends PlayWSClientEngine {

  private object URLTargets {
    val formrecognizer = "formrecognizer"
    val documentintelligence = "documentintelligence"
  }

  override def createURL(
    endpoint: Option[String],
    value: Option[String] = None
  ): String = {
    def apiBasedURL(apiVersion: String) = {
      val slash = if (coreUrl.endsWith("/")) "" else "/"

      import AzureFormRecognizerApiVersion._
      val target = apiVersion match {
        case `v2022_06_30_preview` => URLTargets.formrecognizer
        case `v2022_08_31`         => URLTargets.formrecognizer
        case `v2023_02_28_preview` => URLTargets.formrecognizer
        case `v2023_07_31`         => URLTargets.formrecognizer
        case `v2023_10_31_preview` => URLTargets.documentintelligence
        case `v2024_02_29_preview` => URLTargets.documentintelligence
        case _                     => URLTargets.documentintelligence
      }

      s"${coreUrl}${slash}${target}/documentModels/"
    }

    val endpointString = endpoint.getOrElse("")

    value.map { value =>
      val parts = value.split(",").map(_.trim)

      parts.length match {
        case 2 =>
          val apiVersion = parts(0)
          val modelId = parts(1)
          s"${apiBasedURL(apiVersion)}${modelId}:${endpointString}"

        case 3 =>
          val apiVersion = parts(0)
          val modelId = parts(1)
          val resultsId = parts(2)
          s"${apiBasedURL(apiVersion)}${modelId}/${endpointString}/${resultsId}"
        case _ =>
          throw new AzureFormRecognizerClientException(
            s"AzureFormRecognizerService: createURL: end point param must have 2 or 3 parts separated by comma but got: ${value}"
          )
      }
    }.getOrElse(
      throw new Exception("AzureFormRecognizerService: createURL: endPointParam is None")
    )
  }
}

object AzurePlayWSClientEngine {

  def apply(
    coreUrl: String,
    requestContext: WsRequestContext = WsRequestContext(),
    recoverErrors: String => PartialFunction[Throwable, RichResponse] = defaultRecoverErrors
  )(
    implicit materializer: Materializer,
    ec: ExecutionContext
  ): WSClientEngine =
    new AzurePlayWSClientEngineImpl(coreUrl, requestContext, recoverErrors)

  private final class AzurePlayWSClientEngineImpl(
    endPoint: String,
    override protected val requestContext: WsRequestContext,
    override protected val recoverErrors: String => PartialFunction[Throwable, RichResponse] =
      defaultRecoverErrors
  )(
    override protected implicit val materializer: Materializer,
    override protected implicit val ec: ExecutionContext
  ) extends AzurePlayWSClientEngine(endPoint, requestContext, recoverErrors)(materializer, ec)

  private def defaultRecoverErrors: String => PartialFunction[Throwable, RichResponse] = {
    (serviceEndPointName: String) =>
      {
        case e: TimeoutException =>
          throw new AzureFormRecognizerClientTimeoutException(
            s"${serviceEndPointName} timed out: ${e.getMessage}."
          )
        case e: UnknownHostException =>
          throw new AzureFormRecognizerClientUnknownHostException(
            s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
          )
      }
  }
}
