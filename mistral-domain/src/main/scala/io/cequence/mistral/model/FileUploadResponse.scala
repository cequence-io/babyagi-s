package io.cequence.mistral.model

import java.util.UUID

case class FileUploadResponse(
  id: UUID,
  bytes: Long,
  createdAt: Long,
  filename: String,
  purpose: String,
  sampleType: String,
  numLines: Option[Int],
  source: String
)