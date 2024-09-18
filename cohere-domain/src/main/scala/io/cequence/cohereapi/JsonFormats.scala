package io.cequence.cohereapi

import io.cequence.cohereapi.model._
import io.cequence.wsclient.JsonUtil
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.cequence.wsclient.JsonUtil.enumFormat

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

  // chat
  implicit lazy val connectorFormat: Format[Connector] = {
    implicit val stringAnyMapFormat: Format[Map[String, Any]] = io.cequence.wsclient.JsonUtil.StringAnyMapFormat
    Json.format[Connector]
  }

  implicit lazy val toolCallFormat: Format[ToolCall] = {
    implicit val stringAnyMapFormat: Format[Map[String, Any]] = io.cequence.wsclient.JsonUtil.StringAnyMapFormat
    Json.format[ToolCall]
  }

  implicit lazy val toolCallResultFormat: Format[ToolCallResult] = {
    implicit val stringAnyMapFormat: Format[Map[String, Any]] = io.cequence.wsclient.JsonUtil.StringAnyMapFormat
    Json.format[ToolCallResult]
  }

  implicit lazy val toolResultFormat: Format[ToolResult] = {
    implicit val stringAnyMapFormat: Format[Map[String, Any]] = io.cequence.wsclient.JsonUtil.StringAnyMapFormat
    Json.format[ToolResult]
  }

  implicit lazy val roleFormat: Format[Role] = enumFormat[Role](
    Role.CHATBOT,
    Role.SYSTEM,
    Role.USER,
    Role.TOOL
  )

  implicit lazy val chatMessageFormat: Format[ChatMessage] = new Format[ChatMessage] {
    val chatbotMessageFormat = Json.format[ChatbotMessage]
    val systemMessageFormat = Json.format[SystemMessage]
    val userMessageFormat = Json.format[UserMessage]
    val toolMessageFormat = Json.format[ToolMessage]

    def reads(json: JsValue): JsResult[ChatMessage] = {
      (json \ "role").asOpt[Role] match {
        case Some(role) => role match {
          case Role.CHATBOT => chatbotMessageFormat.reads(json)
          case Role.SYSTEM => systemMessageFormat.reads(json)
          case Role.USER => userMessageFormat.reads(json)
          case Role.TOOL => toolMessageFormat.reads(json)
        }
        case None => JsError("Missing or invalid 'role' field in ChatMessage JSON")
      }
    }
    def writes(message: ChatMessage): JsValue = message match {
      case m: ChatbotMessage => chatbotMessageFormat.writes(m)
      case m: SystemMessage => systemMessageFormat.writes(m)
      case m: UserMessage => userMessageFormat.writes(m)
      case m: ToolMessage => toolMessageFormat.writes(m)
    }
  }

  implicit lazy val promptTruncationFormat = enumFormat[PromptTruncation](
    PromptTruncation.AUTO,
    PromptTruncation.AUTO_PRESERVE_ORDER,
    PromptTruncation.OFF
  )

  implicit lazy val citationQualityFormat: Format[CitationQuality] = enumFormat[CitationQuality](
    CitationQuality.accurate,
    CitationQuality.fast,
    CitationQuality.off
  )

  // response type
  implicit lazy val responseTypeFormat: Format[ResponseType] = new Format[ResponseType] {
    implicit val stringAnyMapFormat: Format[Map[String, Any]] = io.cequence.wsclient.JsonUtil.StringAnyMapFormat

    def reads(json: JsValue): JsResult[ResponseType] = {
      (json \ "type").asOpt[String] match {
        case None => JsError("Missing 'type' field in ResponseType JSON")
        case Some(typeStr) => typeStr match {
          case "text" => JsSuccess(ResponseType.Text)
          case "json_object" =>
            val schema = (json \ "schema").asOpt[Map[String, Any]]
            JsSuccess(ResponseType.JsonObject(schema))
          case other => JsError(s"Unknown ResponseType: $other")
        }
      }
    }

    def writes(responseType: ResponseType): JsValue = responseType match {
      case ResponseType.Text => Json.obj("type" -> "text")
      case ResponseType.JsonObject(schema) =>
        val baseObj = Json.obj("type" -> "json_object")
        schema.fold(baseObj)(s => baseObj + ("schema" -> Json.toJson(s)))
    }
  }

  implicit lazy val chatResponseFormat: Format[ChatResponse] = Json.format[ChatResponse]
  implicit lazy val chatSettingsFormat: Format[ChatSettings] = Json.format[ChatSettings]
}
