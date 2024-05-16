package io.cequence.azureform.service

import io.cequence.wsclient.service.ws.EnumValue

sealed abstract class AzureFormRecognizerEndPoint(value: String = "") extends EnumValue(value)

object AzureFormRecognizerEndPoint {
  case object analyze extends AzureFormRecognizerEndPoint

  case object analyzeResults extends AzureFormRecognizerEndPoint
}

sealed abstract class AzureFormRecognizerParam(value: String = "") extends EnumValue(value)

object AzureFormRecognizerParam {
  case object api_version extends AzureFormRecognizerParam("api-version")
  case object urlSource extends AzureFormRecognizerParam
  case object source extends AzureFormRecognizerParam
  case object base64Source extends AzureFormRecognizerParam
}
