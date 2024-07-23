package io.cequence.azureform.service

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.model._
import io.cequence.wsclient.service.CloseableService

import java.io.File
import scala.concurrent.Future

trait AzureFormRecognizerService extends AzureFormRecognizerConsts with CloseableService {

  def analyze(
    file: File,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[String]

  def analyzeSource(
    source: Source[ByteString, _],
    modelId: String,
  settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[String]

  def analyzeRemote(
    urlSource: String,
    modelId: String,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[String]

  def analyzeReadResults(
    resultsId: String,
    modelId: String = Defaults.readModel
  ): Future[AzureReadResponse]

  def analyzeLayoutResults(
    resultsId: String,
    modelId: String = Defaults.layoutModel
  ): Future[AzureLayoutResponse]

  def analyzeInvoiceResults(
    resultsId: String,
    modelId: String = Defaults.invoiceModel
  ): Future[AzureInvoiceResponse]

  def analyzeRead(
    file: File,
    modelId: String = Defaults.readModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureReadResponse]

  def analyzeLayout(
    file: File,
    modelId: String = Defaults.layoutModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureLayoutResponse]

  def analyzeInvoice(
    file: File,
    modelId: String = Defaults.invoiceModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureInvoiceResponse]

  def analyzeReadSource(
    source: Source[ByteString, _],
    modelId: String = Defaults.readModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureReadResponse]

  def analyzeLayoutSource(
    source: Source[ByteString, _],
    modelId: String = Defaults.layoutModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureLayoutResponse]

  def analyzeInvoiceSource(
    source: Source[ByteString, _],
    modelId: String = Defaults.invoiceModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureInvoiceResponse]

  def analyzeReadRemote(
    urlSource: String,
    modelId: String = Defaults.readModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureReadResponse]

  def analyzeLayoutRemote(
    urlSource: String,
    modelId: String = Defaults.layoutModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureLayoutResponse]

  def analyzeInvoiceRemote(
    urlSource: String,
    modelId: String = Defaults.invoiceModel,
    settings: AzureFormRecognizerAnalyzeSettings = AzureFormRecognizerAnalyzeSettings()
  ): Future[AzureInvoiceResponse]
}