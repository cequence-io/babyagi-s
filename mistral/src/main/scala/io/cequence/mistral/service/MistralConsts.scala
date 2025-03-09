package io.cequence.mistral.service

import io.cequence.mistral.model.OCRSettings

trait MistralConsts {
  object Defaults {
    val OCR = OCRSettings(MistralOCRModel.mistral_ocr_latest)
  }
}