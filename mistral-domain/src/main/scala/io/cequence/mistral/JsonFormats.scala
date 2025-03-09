package io.cequence.mistral

import io.cequence.mistral.model.{Document, FileDeleteResponse, FileInfo, FileListResponse, FileUploadResponse, OCRImage, OCRPage, OCRPageDimensions, OCRResponse, OCRSettings, OCRUsageInfo}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JsonNaming.SnakeCase
import io.cequence.wsclient.JsonUtil.{JsonOps, enumFormat, toJson}

object JsonFormats {

  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit lazy val ocrSettingsFormat: OFormat[OCRSettings] = Json.format[OCRSettings]

  private implicit lazy val documentUrlChunkFormat: OFormat[Document.DocumentURLChunk] =
    Json.format[Document.DocumentURLChunk]

  private implicit lazy val imageUrlChunkFormat: OFormat[Document.ImageURLChunk] =
    Json.format[Document.ImageURLChunk]

  implicit lazy val documentFormat: Format[Document] = {
    val documentUrlChunkType = "document_url"
    val imageUrlChunkType = "image_url"

    val reads: Reads[Document] = (json: JsValue) =>
      (json \ "type").as[String] match {
        case `documentUrlChunkType` => Json.fromJson[Document.DocumentURLChunk](json)
        case `imageUrlChunkType`    => Json.fromJson[Document.ImageURLChunk](json)
        case other                  => JsError(s"Unknown document type: $other")
      }

    val writes: Writes[Document] = Writes {
      case doc: Document.DocumentURLChunk =>
        Json.toJsObject(doc) + ("type" -> JsString(documentUrlChunkType))

      case img: Document.ImageURLChunk =>
        Json.toJsObject(img) + ("type" -> JsString(imageUrlChunkType))
    }

    Format(reads, writes)
  }

  implicit lazy val ocrResponseFormat: Format[OCRResponse] =
    Json.format[OCRResponse]

  implicit lazy val ocrImageFormat: Format[OCRImage] =
    Json.format[OCRImage]

  implicit lazy val ocrPageDimensionsFormat: Format[OCRPageDimensions] =
    Json.format[OCRPageDimensions]

  implicit lazy val ocrUsageInfoFormat: Format[OCRUsageInfo] =
    Json.format[OCRUsageInfo]

  implicit lazy val ocrPageFormat: Format[OCRPage] =
    Json.format[OCRPage]

  implicit lazy val fileUploadResponseFormat: Format[FileUploadResponse] =
    Json.format[FileUploadResponse]

  implicit lazy val fileDeleteResponseFormat: Format[FileDeleteResponse] =
    Json.format[FileDeleteResponse]

  implicit lazy val fileInfoFormat: Format[FileInfo] = Json.format[FileInfo]

  implicit lazy val fileListResponseFormat: Format[FileListResponse] =
    Json.format[FileListResponse]
}
