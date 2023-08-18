package io.cequence.azureform.service

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.cequence.azureform.model._

import java.io.File
import scala.concurrent.Future

trait AzureFormRecognizerService extends AzureFormRecognizerConsts {

  def analyze(
    file: File,
    modelId: String,
    apiVersion: String = Defaults.version
  ): Future[String]

  def analyzeSource(
    source: Source[ByteString, _],
    modelId: String,
    apiVersion: String = Defaults.version
  ): Future[String]

  def analyzeRemote(
    urlSource: String,
    modelId: String,
    apiVersion: String = Defaults.version
  ): Future[String]

  def analyzeReadResults(
    resultsId: String,
    modelId: String = Defaults.readModel,
    apiVersion: String = Defaults.version
  ): Future[AzureReadResponse]

  def analyzeLayoutResults(
    resultsId: String,
    modelId: String = Defaults.layoutModel,
    apiVersion: String = Defaults.version
  ): Future[AzureLayoutResponse]

  def analyzeInvoiceResults(
    resultsId: String,
    modelId: String = Defaults.invoiceModel,
    apiVersion: String = Defaults.version
  ): Future[AzureInvoiceResponse]

  def analyzeRead(
    file: File,
    modelId: String = Defaults.readModel,
    apiVersion: String = Defaults.version
  ): Future[AzureReadResponse]

  def analyzeLayout(
    file: File,
    modelId: String = Defaults.layoutModel,
    apiVersion: String = Defaults.version
  ): Future[AzureLayoutResponse]

  def analyzeInvoice(
    file: File,
    modelId: String = Defaults.invoiceModel,
    apiVersion: String = Defaults.version
  ): Future[AzureInvoiceResponse]

  def analyzeReadSource(
    source: Source[ByteString, _],
    modelId: String = Defaults.readModel,
    apiVersion: String = Defaults.version
  ): Future[AzureReadResponse]

  def analyzeLayoutSource(
    source: Source[ByteString, _],
    modelId: String = Defaults.layoutModel,
    apiVersion: String = Defaults.version
  ): Future[AzureLayoutResponse]

  def analyzeInvoiceSource(
    source: Source[ByteString, _],
    modelId: String = Defaults.invoiceModel,
    apiVersion: String = Defaults.version
  ): Future[AzureInvoiceResponse]

  def analyzeReadRemote(
    urlSource: String,
    modelId: String = Defaults.readModel,
    apiVersion: String = Defaults.version
  ): Future[AzureReadResponse]

  def analyzeLayoutRemote(
    urlSource: String,
    modelId: String = Defaults.layoutModel,
    apiVersion: String = Defaults.version
  ): Future[AzureLayoutResponse]

  def analyzeInvoiceRemote(
    urlSource: String,
    modelId: String = Defaults.invoiceModel,
    apiVersion: String = Defaults.version
  ): Future[AzureInvoiceResponse]

  def close(): Unit
}