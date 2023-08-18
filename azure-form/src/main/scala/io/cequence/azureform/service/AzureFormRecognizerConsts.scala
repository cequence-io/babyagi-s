package io.cequence.azureform.service

import io.cequence.azureform.model.{AzureFormRecognizerApiVersion, AzureFormRecognizerModelId}

trait AzureFormRecognizerConsts {
  object Defaults {
    val version = AzureFormRecognizerApiVersion.v2023_02_28_preview
    val readModel = AzureFormRecognizerModelId.prebuilt_read
    val layoutModel = AzureFormRecognizerModelId.prebuilt_layout
    val invoiceModel = AzureFormRecognizerModelId.prebuilt_invoice
  }
}