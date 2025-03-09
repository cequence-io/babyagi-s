package io.cequence.mistral.service

import io.cequence.mistral.model.{Document, FileDeleteResponse, FileListResponse, FileUploadResponse, OCRResponse, OCRSettings}
import io.cequence.wsclient.service.CloseableService

import java.util.UUID
import scala.concurrent.Future

trait MistralService extends MistralConsts with CloseableService {

  def ocr(
    document: Document,
    settings: OCRSettings = Defaults.OCR
  ): Future[OCRResponse]

  def uploadWithOCR(
    file: java.io.File,
    settings: OCRSettings = Defaults.OCR
  ): Future[OCRResponse]

  def uploadFile(
    file: java.io.File,
    purpose: Option[String]
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
}
