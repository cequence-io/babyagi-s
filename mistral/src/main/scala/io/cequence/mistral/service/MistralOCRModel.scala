package io.cequence.mistral.service

object MistralOCRModel {
  @deprecated("Retired by Mistral on 2025-12-31 - use mistral_ocr_4_1")
  val mistral_ocr_2503 = "mistral-ocr-2503" // OCR 1
  @deprecated("Retired by Mistral on 2026-05-31 - use mistral_ocr_4_1")
  val mistral_ocr_2505 = "mistral-ocr-2505" // OCR 2
  val mistral_ocr_2512 = "mistral-ocr-2512" // OCR 3
  @deprecated("Retired by Mistral on 2026-09-30 - use mistral_ocr_4_1")
  val mistral_ocr_4_0 = "mistral-ocr-4-0" // OCR 4
  val mistral_ocr_4_1 = "mistral-ocr-4-1" // OCR 4.1
  val mistral_ocr_latest = "mistral-ocr-latest"
}
