package io.cequence.azureform.model

object AzureFormRecognizerApiVersion {
  val v2024_11_30 = "2024-11-30" // v4.0 (GA)
  @deprecated("Retired by Azure on 2026-06-30 - use v2024_11_30")
  val v2024_07_31_preview = "2024-07-31-preview" // v4.0
  @deprecated("Retired by Azure on 2026-06-30 - use v2024_11_30")
  val v2024_02_29_preview = "2024-02-29-preview" // v4.0
  @deprecated("Retired by Azure on 2026-06-30 - use v2024_11_30")
  val v2023_10_31_preview = "2023-10-31-preview" // v4.0
  val v2023_07_31 = "2023-07-31" // v3.1
  @deprecated("Retired by Azure - use v2024_11_30")
  val v2023_02_28_preview = "2023-02-28-preview"
  val v2022_08_31 = "2022-08-31" // v3
  @deprecated // not supported anymore
  val v2022_06_30_preview = "2022-06-30-preview"
}
