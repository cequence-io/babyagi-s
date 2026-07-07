package io.cequence.mistral.model

case class BatchJob(
  id: String,
  status: String,
  endpoint: String,
  model: Option[String] = None,
  inputFiles: Seq[String] = Nil,
  totalRequests: Int,
  succeededRequests: Int,
  failedRequests: Int,
  completedRequests: Int,
  createdAt: Long,
  startedAt: Option[Long] = None,
  completedAt: Option[Long] = None,
  outputFile: Option[String] = None,
  errorFile: Option[String] = None,
  errors: Seq[BatchJobError] = Nil,
  metadata: Option[Map[String, String]] = None
)

case class BatchJobError(
  message: String
)

case class BatchJobList(
  data: Seq[BatchJob],
  total: Int
)

// wire-format request for POST /v1/batch/jobs
case class BatchJobRequest(
  endpoint: String,
  model: Option[String] = None,
  inputFiles: Option[Seq[String]] = None,
  metadata: Option[Map[String, String]] = None,
  timeoutHours: Option[Int] = None
)

// A single OCR document submitted as a part of a batch job
case class OCRBatchItem(
  customId: String,
  document: Document,
  settings: Option[OCRSettings] = None // overrides the batch-level default settings if provided
)

// Parsed result of a single OCR batch item once the batch job has finished
case class OCRBatchItemResult(
  customId: String,
  ocrResponse: Option[OCRResponse] = None,
  errorMessage: Option[String] = None
)
