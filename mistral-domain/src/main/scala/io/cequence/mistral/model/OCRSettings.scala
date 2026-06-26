package io.cequence.mistral.model

import play.api.libs.json.JsObject

case class OCRSettings(
  model: String,
  pages: Seq[Int] = Seq.empty,
  includeImageBase64: Option[Boolean] = None,
  imageLimit: Option[Int] = None,
  imageMinSize: Option[Int] = None,
  // OCR 3/4-era params (mistral-ocr-2512 / mistral-ocr-4-0)
  tableFormat: Option[String] = None, // "markdown" | "html"
  extractHeader: Option[Boolean] = None,
  extractFooter: Option[Boolean] = None,
  includeBlocks: Option[Boolean] = None, // paragraph-level block bounding boxes
  confidenceScoresGranularity: Option[String] = None, // "word" | "page"
  bboxAnnotationFormat: Option[JsObject] = None, // ResponseFormat (json_schema only)
  documentAnnotationFormat: Option[JsObject] = None, // ResponseFormat (json_schema only)
  documentAnnotationPrompt: Option[String] = None
)

sealed trait Document

object Document {
  case class DocumentURLChunk(
    documentUrl: String,
    documentName: String
  ) extends Document

  case class ImageURLChunk(
    imageUrl: String
  ) extends Document
}
