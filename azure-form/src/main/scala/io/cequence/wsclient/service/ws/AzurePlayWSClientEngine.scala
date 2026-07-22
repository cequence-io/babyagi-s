package io.cequence.wsclient.service.ws

import akka.stream.Materializer
import io.cequence.azureform.model.AzureFormRecognizerApiVersion
import io.cequence.azureform.AzureFormRecognizerClientException
import io.cequence.wsclient.domain.SiteBinding
import io.cequence.wsclient.service.{WSClientEngine, WSClientInputStreamExtraAkka}
import io.cequence.wsclient.service.spi.TransportSettings
import play.api.libs.ws.StandaloneWSRequest

import scala.concurrent.ExecutionContext

/**
 * A CUSTOM Play WS engine subclass overriding `createURL` (Azure's api-version-dependent URL
 * scheme). NOTE: deliberately NOT migrated to engine self-discovery (`WSClientEngineRegistry`)
 * \- the discovery SPI creates stock engines, while this behavior lives in an engine subclass;
 * making it backend-agnostic would require lifting the URL logic into the service layer.
 */
class AzurePlayWSClientEngine(
  transportSettings: TransportSettings = TransportSettings()
)(
  implicit materializer: Materializer,
  ec: ExecutionContext
) extends PlayWSClientEngine(transportSettings) {

  private object URLTargets {
    val formrecognizer = "formrecognizer"
    val documentintelligence = "documentintelligence"
  }

  private def createAzureURL(
    coreUrl: String,
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

  override protected[ws] def getWSRequestOptional(
    site: SiteBinding,
    endPoint: Option[String],
    endPointParam: Option[String],
    params: Seq[(String, Option[Any])],
    extraHeaders: Seq[(String, String)]
  ): StandaloneWSRequest#Self =
    super.getWSRequestOptional(
      site.copy(coreUrl = createAzureURL(site.coreUrl, endPoint, endPointParam)),
      endPoint = None,
      endPointParam = None,
      params,
      extraHeaders
    )
}

object AzurePlayWSClientEngine {

  def apply(
    transportSettings: TransportSettings = TransportSettings()
  )(
    implicit materializer: Materializer,
    ec: ExecutionContext
  ): WSClientEngine with WSClientInputStreamExtraAkka =
    new AzurePlayWSClientEngine(transportSettings)
}
