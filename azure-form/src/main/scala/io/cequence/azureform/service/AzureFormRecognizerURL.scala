package io.cequence.azureform.service

import io.cequence.azureform.AzureFormRecognizerClientException
import io.cequence.azureform.model.AzureFormRecognizerApiVersion

/**
 * Azure Form Recognizer / Document Intelligence's api-version-dependent URL scheme: v3.x and
 * older use the `formrecognizer` URL segment, v4.0+ (2023-10-31-preview and later)
 * `documentintelligence`. The endpoint param carries `apiVersion,modelId` (analyze) or
 * `apiVersion,modelId,resultsId` (analyze results). In 1.x this lived in an engine subclass
 * (`AzurePlayWSClientEngine`), which tied the service to Play WS on Akka.
 */
private[service] object AzureFormRecognizerURL {

  private val formRecognizer = "formrecognizer"
  private val documentIntelligence = "documentintelligence"

  def apply(
    coreUrl: String,
    endpoint: String,
    endPointParam: Option[String]
  ): String = {
    val value = endPointParam.getOrElse(
      throw new AzureFormRecognizerClientException(
        "AzureFormRecognizerService: createURL: endPointParam is None"
      )
    )

    val parts = value.split(",").map(_.trim)

    parts.length match {
      case 2 =>
        val apiVersion = parts(0)
        val modelId = parts(1)
        s"${apiBasedURL(coreUrl, apiVersion)}${modelId}:${endpoint}"

      case 3 =>
        val apiVersion = parts(0)
        val modelId = parts(1)
        val resultsId = parts(2)
        s"${apiBasedURL(coreUrl, apiVersion)}${modelId}/${endpoint}/${resultsId}"

      case _ =>
        throw new AzureFormRecognizerClientException(
          s"AzureFormRecognizerService: createURL: end point param must have 2 or 3 parts separated by comma but got: ${value}"
        )
    }
  }

  private def apiBasedURL(
    coreUrl: String,
    apiVersion: String
  ): String = {
    val slash = if (coreUrl.endsWith("/")) "" else "/"

    import AzureFormRecognizerApiVersion._
    val target = apiVersion match {
      case `v2022_06_30_preview` => formRecognizer
      case `v2022_08_31`         => formRecognizer
      case `v2023_02_28_preview` => formRecognizer
      case `v2023_07_31`         => formRecognizer
      case _                     => documentIntelligence
    }

    s"${coreUrl}${slash}${target}/documentModels/"
  }
}
