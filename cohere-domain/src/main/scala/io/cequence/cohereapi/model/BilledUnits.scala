package io.cequence.cohereapi.model

case class BilledUnits(
  search_units: Option[Int],
  classifications: Option[Int],
  input_tokens: Option[Int]
)
