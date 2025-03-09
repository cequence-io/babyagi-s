package io.cequence.mistral.model

case class FileListResponse(
  data: List[FileInfo],
  total: Int
)

case class FileInfo(
  id: String,
  bytes: Long,
  createdAt: Long,
  filename: String,
  purpose: String,
  sampleType: String,
  numLines: Option[Int],
  source: String
)
