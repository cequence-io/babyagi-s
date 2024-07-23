package io.cequence.azureform.service

import io.cequence.wsclient.domain.NamedEnumValue

sealed abstract class AzureFormRecognizerEndPoint(value: String = "")
    extends NamedEnumValue(value)

object AzureFormRecognizerEndPoint {
  case object analyze extends AzureFormRecognizerEndPoint

  case object analyzeResults extends AzureFormRecognizerEndPoint
}

sealed abstract class AzureFormRecognizerParam(value: String = "")
    extends NamedEnumValue(value)

object AzureFormRecognizerParam {
  case object api_version extends AzureFormRecognizerParam("api-version")
  case object urlSource extends AzureFormRecognizerParam
  case object source extends AzureFormRecognizerParam
  case object base64Source extends AzureFormRecognizerParam
  case object pages extends AzureFormRecognizerParam
  case object overload extends AzureFormRecognizerParam("_overload")
  case object features extends AzureFormRecognizerParam
  case object outputContentFormat extends AzureFormRecognizerParam
}
