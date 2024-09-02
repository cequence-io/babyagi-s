package io.cequence.cohereapi

import io.cequence.cohereapi.model._
import io.cequence.wsclient.JsonUtil
import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonFormats {
  implicit lazy val apiVersionFormat: Format[ApiVersion] = Json.format[ApiVersion]
  implicit lazy val billedUnitsFormat: Format[BilledUnits] =
    Json.format[BilledUnits]
  implicit lazy val responseMetaFormat: Format[ResponseMeta] = Json.format[ResponseMeta]

  // rerank
  implicit lazy val rerankResultFormat: Format[RerankResult] = {
    implicit lazy val stringAnyMapFormat: Format[Map[String, Any]] =
      JsonUtil.StringAnyMapFormat
    Json.format[RerankResult]
  }
  implicit lazy val rerankResponseFormat: Format[RerankResponse] =
    Json.format[RerankResponse]

  // classify
  implicit lazy val labelConfidenceFormat: Format[LabelConfidence] =
    Json.format[LabelConfidence]
  implicit lazy val classifyResultFormat: Format[ClassifyResult] = Json.format[ClassifyResult]
  implicit lazy val classifyResponseFormat: Format[ClassifyResponse] =
    Json.format[ClassifyResponse]

  // embed
  implicit lazy val embedResponseFormat: Format[EmbedResponse] = Json.format[EmbedResponse]
}
