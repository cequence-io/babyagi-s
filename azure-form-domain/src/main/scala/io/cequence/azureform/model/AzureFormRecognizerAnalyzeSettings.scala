package io.cequence.azureform.model

case class AzureFormRecognizerAnalyzeSettings(
  pages: Option[String] = None,
  features: Seq[String] = Nil,
  outputContentFormat: Option[ContentFormat] = None
)
