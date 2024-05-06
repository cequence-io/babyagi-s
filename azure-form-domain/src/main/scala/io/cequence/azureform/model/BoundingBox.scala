package io.cequence.azureform.model

case class BoundingBox(
  minX: Double,
  minY: Double,
  maxX: Double,
  maxY: Double
)

case class BoundingBoxWithPage(
  pageIndex: Int,
  refPageWidth: Double,
  refPageHeight: Double,
  boundingBox: BoundingBox
)

case class BufferedImageSource(
  width: Int,
  height: Int,
  source: Array[Byte]
)
