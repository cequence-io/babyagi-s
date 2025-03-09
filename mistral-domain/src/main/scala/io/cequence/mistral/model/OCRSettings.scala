package io.cequence.mistral.model

case class OCRSettings(
  model: String,
  id: Option[String] = None,
  pages: Seq[Int] = Seq.empty,
  includeImageBase64: Option[Boolean] = None,
  imageLimit: Option[Int] = None,
  imageMinSize: Option[Int] = None
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
