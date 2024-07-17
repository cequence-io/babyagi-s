package io.cequence.azureform.model

case class AzureLayoutResponse(
  status: String,
  createdDateTime: String,
  lastUpdatedDateTime: String,
  analyzeResult: Option[LayoutAnalyzeResult]
) extends HasStatus

case class LayoutAnalyzeResult(
  apiVersion: String,
  modelId: String,
  stringIndexType: String,
  content: String,
  pages: Seq[LayoutPage], // page with selection marks
  paragraphs: Seq[Paragraph],
  tables: Seq[Table] // extra: compared to ReadAnalyzeResult
  // TODO: add styles
)

case class LayoutPage(
  pageNumber: Int,
  angle: Double,
  width: Double,
  height: Double,
  unit: String,
  spans: Seq[Span],
  kind: Option[String],
  words: Seq[Word],
  lines: Seq[Line],
  selectionMarks: Option[Seq[SelectionMark]]
)
case class Table(
  boundingRegions: List[BoundingRegion],
  spans: List[Span],
  cells: List[Cell],
  rowCount: Int,
  columnCount: Int
)

case class Cell(
  boundingRegions: List[BoundingRegion],
  spans: List[Span],
  kind: Option[String],
  rowIndex: Int,
  columnIndex: Int,
  content: String
)

case class SelectionMark(
  polygon: Seq[Double],
  confidence: Double,
  state: String,
  span: Span
)
