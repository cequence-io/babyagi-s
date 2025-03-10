package io.cequence.mistral.service

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.mistral.model.{Document, FileDeleteResponse, FileListResponse, FileUploadResponse, OCRResponse, OCRSettings}
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
}
