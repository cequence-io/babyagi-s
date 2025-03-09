package io.cequence.mistral.model

case class OCRResponse(
  pages: Seq[OCRPage],
  model: String,
  usageInfo: OCRUsageInfo
)

case class OCRPage(
  index: Int,
  markdown: String,
  images: Seq[OCRImage],
  dimensions: Option[OCRPageDimensions]
)

case class OCRImage(
  id: String,
  topLeftX: Option[Int],
  topLeftY: Option[Int],
  bottomRightX: Option[Int],
  bottomRightY: Option[Int],
  imageBase64: Option[String]
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
