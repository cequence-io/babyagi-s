package io.cequence.azureform.service

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.model.{
  AzureFormRecognizerAnalyzeSettings,
  AzureInvoiceResponse,
  AzureLayoutResponse,
  AzureReadResponse
}
import io.cequence.wsclient.service.adapter.DelegatedCloseableServiceWrapper
import io.cequence.wsclient.service.adapter.ServiceWrapperTypes.CloseableServiceWrapper

import java.io.File
import scala.concurrent.Future

object AzureFormRecognizerServiceWrapper {

  def apply(
    delegate: CloseableServiceWrapper[AzureFormRecognizerService]
  ): AzureFormRecognizerService =
    new AzureFormRecognizerServiceWrapperImpl(delegate)

  private final class AzureFormRecognizerServiceWrapperImpl(
    val delegate: CloseableServiceWrapper[AzureFormRecognizerService]
  ) extends AzureFormRecognizerServiceWrapper

  trait AzureFormRecognizerServiceWrapper
      extends DelegatedCloseableServiceWrapper[
        AzureFormRecognizerService,
        CloseableServiceWrapper[
          AzureFormRecognizerService
        ]
      ]
      with AzureFormRecognizerService {

    override def analyze(
      file: File,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[String] =
      wrap(
        _.analyze(file, modelId, settings)
      )

    override def analyzeSource(
      source: Source[ByteString, _],
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[String] =
      wrap(
        _.analyzeSource(source, modelId, settings)
      )

    override def analyzeRemote(
      urlSource: String,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[String] =
      wrap(
        _.analyzeRemote(urlSource, modelId, settings)
      )

    override def analyzeReadResults(
      resultsId: String,
      modelId: String
    ): Future[AzureReadResponse] =
      wrap(
        _.analyzeReadResults(resultsId, modelId)
      )

    override def analyzeLayoutResults(
      resultsId: String,
      modelId: String
    ): Future[AzureLayoutResponse] =
      wrap(
        _.analyzeLayoutResults(resultsId, modelId)
      )

    override def analyzeInvoiceResults(
      resultsId: String,
      modelId: String
    ): Future[AzureInvoiceResponse] =
      wrap(
        _.analyzeInvoiceResults(resultsId, modelId)
      )

    override def analyzeRead(
      file: File,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureReadResponse] =
      wrap(
        _.analyzeRead(file, modelId, settings)
      )

    override def analyzeLayout(
      file: File,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureLayoutResponse] =
      wrap(
        _.analyzeLayout(file, modelId, settings)
      )

    override def analyzeInvoice(
      file: File,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureInvoiceResponse] =
      wrap(
        _.analyzeInvoice(file, modelId, settings)
      )

    override def analyzeReadSource(
      source: Source[ByteString, _],
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureReadResponse] =
      wrap(
        _.analyzeReadSource(source, modelId, settings)
      )

    override def analyzeLayoutSource(
      source: Source[ByteString, _],
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureLayoutResponse] =
      wrap(
        _.analyzeLayoutSource(source, modelId, settings)
      )

    override def analyzeInvoiceSource(
      source: Source[ByteString, _],
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureInvoiceResponse] =
      wrap(
        _.analyzeInvoiceSource(source, modelId, settings)
      )

    override def analyzeReadRemote(
      urlSource: String,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureReadResponse] =
      wrap(
        _.analyzeReadRemote(urlSource, modelId, settings)
      )

    override def analyzeLayoutRemote(
      urlSource: String,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureLayoutResponse] =
      wrap(
        _.analyzeLayoutRemote(urlSource, modelId, settings)
      )

    override def analyzeInvoiceRemote(
      urlSource: String,
      modelId: String,
      settings: AzureFormRecognizerAnalyzeSettings
    ): Future[AzureInvoiceResponse] =
      wrap(
        _.analyzeInvoiceRemote(urlSource, modelId, settings)
      )
  }
}