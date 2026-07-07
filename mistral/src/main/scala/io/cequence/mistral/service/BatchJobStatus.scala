package io.cequence.mistral.service

object BatchJobStatus {
  val Queued = "QUEUED"
  val Running = "RUNNING"
  val Success = "SUCCESS"
  val Failed = "FAILED"
  val TimeoutExceeded = "TIMEOUT_EXCEEDED"
  val CancellationRequested = "CANCELLATION_REQUESTED"
  val Cancelled = "CANCELLED"

  // statuses after which a batch job no longer changes and results (or errors) can be fetched
  val terminal: Set[String] = Set(Success, Failed, TimeoutExceeded, Cancelled)
}

object BatchEndpoint {
  val ocr = "/v1/ocr"
}
