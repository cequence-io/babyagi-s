package io.cequence.azureform.model

case class AzureReadResponse(
  status: String,
  createdDateTime: String,
  lastUpdatedDateTime: String,
  analyzeResult: Option[ReadAnalyzeResult]
) extends HasStatus

trait HasStatus {
  def status: String
}

case class ReadAnalyzeResult(
  apiVersion: String,
  modelId: String,
  stringIndexType: String,
  content: String,
  pages: Seq[Page],
  languages: Seq[Language],
  paragraphs: Seq[Paragraph]
)

case class Span(
  offset: Int,
  length: Int
)
case class Word(
  polygon: Seq[Double],
  span: Span,
  confidence: Double,
  content: String
)

case class Line(
  polygon: Seq[Double],
  spans: Seq[Span],
  content: String
)

case class Page(
  pageNumber: Int,
  angle: Double,
  width: Double,
  height: Double,
  unit: String,
  spans: Seq[Span],
  kind: String,
  words: Seq[Word],
  lines: Seq[Line]
)

case class Language(
  spans: Seq[Span],
  confidence: Double,
  locale: String
)

case class BoundingRegion(
  pageNumber: Int,
  // top-left, top-right, bottom-right, bottom-left
  polygon: Seq[Double]
)

case class Paragraph(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  content: String
)
