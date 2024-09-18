package io.cequence.jinaapi

import io.cequence.jinaapi.model._
import io.cequence.wsclient.JsonUtil
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.cequence.wsclient.JsonUtil.{JsonOps, enumFormat, toJson}

object JsonFormats {

  // crawler
  implicit lazy val contentFormatFormat: Format[ContentFormat] = enumFormat[ContentFormat](
    ContentFormat.markdown,
    ContentFormat.html,
    ContentFormat.text,
    ContentFormat.screenshot,
    ContentFormat.pageshot
  )

  implicit lazy val crawlerSettingsFormat: Format[CrawlerSettings] = Json.format[CrawlerSettings]
  implicit lazy val usageFormat: Format[Usage] = Json.format[Usage]
  implicit lazy val crawlDataFormat: Format[CrawlData] = Json.format[CrawlData]
  implicit lazy val crawlResponseFormat: Format[CrawlResponse] = Json.format[CrawlResponse]

  // segmenter
  implicit lazy val segmenterSettingsFormat: Format[SegmenterSettings] = Json.format[SegmenterSettings]
  implicit lazy val segmenterDataFormat: Format[SegmenterData] = Json.format[SegmenterData]

  // TODO: move to ws client
  implicit lazy val segmenterResponseFormat: Format[SegmenterResponse] = {
    implicit object SeqAnyFormat extends Format[Seq[Any]] {
      override def reads(json: JsValue): JsResult[Seq[Any]] = {
        val maybeJsons = json.asSafe[JsArray].value.map(JsonUtil.toValue)
        if (maybeJsons.exists(_.isEmpty)) {
          JsError("One or more elements in the array are null")
        } else {
          JsSuccess(maybeJsons.map(_.get))
        }
      }

      override def writes(o: Seq[Any]): JsValue = {
        val jsValues = o.map(toJson)
        JsArray(jsValues)
      }
    }

    Json.format[SegmenterResponse]
  }

  // reranker
  implicit lazy val rerankerSettingsFormat: Format[RerankerSettings] = Json.format[RerankerSettings]
  implicit lazy val rerankerUsageFormat: Format[RerankerUsage] = Json.format[RerankerUsage]
  implicit lazy val rerankerResultFormat: Format[RerankerResult] = Json.format[RerankerResult]
  implicit lazy val rerankerDocumentFormat: Format[RerankerDocument] = Json.format[RerankerDocument]
  implicit lazy val rerankerResponseFormat: Format[RerankerResponse] = Json.format[RerankerResponse]
}
