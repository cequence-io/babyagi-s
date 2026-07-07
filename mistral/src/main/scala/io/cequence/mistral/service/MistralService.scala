package io.cequence.mistral.service

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.mistral.model.{BatchJob, BatchJobList, Document, FileDeleteResponse, FileListResponse, FileUploadResponse, OCRBatchItem, OCRBatchItemResult, OCRResponse, OCRSettings}
import io.cequence.wsclient.service.CloseableService

import java.util.UUID
import scala.concurrent.Future

trait MistralService extends MistralConsts with CloseableService {

  def ocr(
    document: Document,
    settings: OCRSettings = Defaults.OCR
  ): Future[OCRResponse]

  def ocrWithPages(
    document: Document,
    settings: OCRSettings = Defaults.OCR,
    pageIntervals: Seq[(Int, Int)]
  ): Future[OCRResponse]

  def uploadFile(
    file: java.io.File,
    purpose: Option[String],
    fileName: Option[String] = None
  ): Future[FileUploadResponse]

  def uploadSource(
    source: Source[ByteString, _],
    purpose: Option[String],
    fileName: Option[String] = None
  ): Future[FileUploadResponse]

  def deleteFile(
    fileId: UUID
  ): Future[FileDeleteResponse]

  def listFiles(
    page: Option[Int] = None,
    pageSize: Option[Int] = None
  ): Future[FileListResponse]

  def signFileURL(
    fileId: UUID,
    expiryHours: Int
  ): Future[String]

  def uploadWithOCR(
    file: java.io.File,
    settings: OCRSettings = Defaults.OCR,
    pageIntervals: Seq[(Int, Int)] = Nil,
    fileName: Option[String] = None
  ): Future[OCRResponse]

  def uploadSourceWithOCR(
    source: Source[ByteString, _],
    settings: OCRSettings = Defaults.OCR,
    pageIntervals: Seq[(Int, Int)] = Nil,
    fileName: Option[String] = None
  ): Future[OCRResponse]

  def downloadFileContent(
    fileId: UUID
  ): Future[String]

  /////////
  // Batch: OCR (and other endpoints) processed asynchronously at a 50% pricing discount //
  /////////

  def createBatchJob(
    endpoint: String,
    inputFileId: UUID,
    model: Option[String] = None,
    metadata: Map[String, String] = Map.empty,
    timeoutHours: Option[Int] = None
  ): Future[BatchJob]

  def getBatchJob(
    jobId: String
  ): Future[BatchJob]

  def listBatchJobs(
    page: Option[Int] = None,
    pageSize: Option[Int] = None,
    createdByMe: Option[Boolean] = None
  ): Future[BatchJobList]

  def cancelBatchJob(
    jobId: String
  ): Future[BatchJob]

  // polls (non-blocking) the job until it reaches a terminal status - see BatchJobStatus.terminal
  def awaitBatchJob(
    jobId: String
  ): Future[BatchJob]

  // submits a batch of OCR documents as a single, 50%-cheaper async job; use awaitBatchJob +
  // ocrBatchResults (or uploadWithOCRBatch for the all-in-one flow) to obtain the results
  def ocrBatch(
    items: Seq[OCRBatchItem],
    settings: OCRSettings = Defaults.OCR,
    metadata: Map[String, String] = Map.empty,
    timeoutHours: Option[Int] = None
  ): Future[BatchJob]

  // downloads and parses the output (and error) file of a finished OCR batch job
  def ocrBatchResults(
    job: BatchJob
  ): Future[Seq[OCRBatchItemResult]]

  // uploads the given files, submits them as a single OCR batch job, awaits its completion,
  // and returns the parsed per-file results; all the intermediate files (uploads, batch
  // input/output) are deleted afterwards
  def uploadWithOCRBatch(
    files: Seq[(String, java.io.File)],
    settings: OCRSettings = Defaults.OCR,
    metadata: Map[String, String] = Map.empty,
    // batch jobs may take up to 24h (the max timeout_hours) to complete, so the signed
    // URLs backing the input files must outlive that window
    signedUrlExpiryHours: Int = 25
  ): Future[Seq[OCRBatchItemResult]]
}
