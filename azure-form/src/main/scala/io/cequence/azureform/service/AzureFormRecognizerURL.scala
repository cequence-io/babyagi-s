package io.cequence.azureform.service

import io.cequence.azureform.model.AzureFormRecognizerApiVersion

/**
 * Azure Form Recognizer / Document Intelligence's api-version-dependent URL scheme: v3.x and
 * older use the `formrecognizer` URL segment, v4.0+ (2023-10-31-preview and later)
 * `documentintelligence`. The service's core URL is the `documentModels` base, under which
 * the endpoints are `{modelId}:analyze` and `{modelId}/analyzeResults/{resultsId}`. In 1.x
 * this lived in an engine subclass (`AzurePlayWSClientEngine`), which tied the service to Play
 * WS on Akka.
 */
private[service] object AzureFormRecognizerURL {

  private val formRecognizer = "formrecognizer"
  private val documentIntelligence = "documentintelligence"

  def documentModels(
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

    s"${coreUrl}${slash}${target}/documentModels"
  }
}
