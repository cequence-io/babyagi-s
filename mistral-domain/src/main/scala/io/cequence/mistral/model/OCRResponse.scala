package io.cequence.mistral.model

case class OCRResponse(
  pages: Seq[OCRPage],
  model: String,
  usageInfo: OCRUsageInfo,
  // Whole-document structured annotation (json str) when document_annotation_format is set
  documentAnnotation: Option[String] = None
)

case class OCRPage(
  index: Int,
  markdown: String,
  images: Seq[OCRImage],
  dimensions: Option[OCRPageDimensions],
  // OCR 3/4-era fields
  tables: Option[Seq[OCRTable]] = None,
  hyperlinks: Option[Seq[String]] = None,
  header: Option[String] = None,
  footer: Option[String] = None,
  confidenceScores: Option[OCRPageConfidenceScores] = None, // confidence_scores_granularity set
  blocks: Option[Seq[OCRBlock]] = None // include_blocks = true
)

case class OCRImage(
  id: String,
  topLeftX: Option[Int],
  topLeftY: Option[Int],
  bottomRightX: Option[Int],
  bottomRightY: Option[Int],
  imageBase64: Option[String],
  // Per-image structured annotation (json str) when bbox_annotation_format is set
  imageAnnotation: Option[String] = None
)

case class OCRPageDimensions(
  dpi: Int,
  height: Int,
  width: Int
)

case class OCRUsageInfo(
  pagesProcessed: Int,
  docSizeBytes: Option[Int]
)

// OCR 3/4: a table extracted from a page
case class OCRTable(
  id: String,
  content: String, // table content in the requested table_format
  format: String, // "markdown" | "html"
  wordConfidenceScores: Option[Seq[OCRConfidenceScore]] = None
)

// OCR 3/4: a paragraph-level content block with its bounding box (include_blocks = true)
case class OCRBlock(
  `type`: String, // text | title | table | image | header | footer | caption | code | ...
  topLeftX: Int,
  topLeftY: Int,
  bottomRightX: Int,
  bottomRightY: Int,
  content: String,
  imageId: Option[String] = None, // image blocks reference an entry in OCRPage.images
  tableId: Option[String] = None // table blocks reference an entry in OCRPage.tables
)

// OCR 3/4: per-page confidence scores (confidence_scores_granularity = "page" | "word")
case class OCRPageConfidenceScores(
  averagePageConfidenceScore: Double,
  minimumPageConfidenceScore: Double,
  wordConfidenceScores: Option[Seq[OCRConfidenceScore]] = None // only for "word" granularity
)

// OCR 3/4: confidence score for a word/text segment
case class OCRConfidenceScore(
  text: String,
  confidence: Double, // 0..1
  startIndex: Int // start offset in the page markdown string
)
