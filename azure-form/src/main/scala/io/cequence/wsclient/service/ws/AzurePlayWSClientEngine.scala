package io.cequence.wsclient.service.ws
import akka.stream.Materializer
import io.cequence.wsclient.domain.{
  CequenceWSTimeoutException,
  CequenceWSUnknownHostException,
  RichResponse,
  WsRequestContext
}
import io.cequence.wsclient.service.WSClientEngine

import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import scala.concurrent.ExecutionContext

class AzurePlayWSClientEngine(
  endPoint: String,
  override protected val requestContext: WsRequestContext,
  override protected val recoverErrors: String => PartialFunction[Throwable, RichResponse]
)(
  override protected implicit val materializer: Materializer,
  override protected implicit val ec: ExecutionContext
) extends PlayWSClientEngine {

  override protected val coreUrl = s"$endPoint/formrecognizer/documentModels/"

  override def createURL(
    endpoint: Option[String],
    value: Option[String] = None
  ): String = {
    val endpointString = endpoint.map(_.toString).getOrElse("")
    value
      .map(value =>
        if (value.contains(",")) {
          val parts = value.split(",").map(_.trim)
          val modelId = parts(0)
          val resultsId = parts(1)

          s"${coreUrl}${modelId}/${endpointString}/${resultsId}"
        } else
          s"${coreUrl}${value}:${endpointString}"
      )
      .getOrElse(
        throw new Exception("AzureFormRecognizerService: createUrl: endPointParam is None")
      )
  }

}

object AzurePlayWSClientEngine {

  def apply(
    endPoint: String,
    requestContext: WsRequestContext = WsRequestContext(),
    recoverErrors: String => PartialFunction[Throwable, RichResponse] = defaultRecoverErrors
  )(
    implicit materializer: Materializer,
    ec: ExecutionContext
  ): WSClientEngine =
    new AzurePlayWSClientEngineImpl(endPoint, requestContext, recoverErrors)

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
    serviceEndPointName: String =>
      {
        case e: TimeoutException =>
          throw new CequenceWSTimeoutException(
            s"${serviceEndPointName} timed out: ${e.getMessage}."
          )
        case e: UnknownHostException =>
          throw new CequenceWSUnknownHostException(
            s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
          )
      }
  }
}
