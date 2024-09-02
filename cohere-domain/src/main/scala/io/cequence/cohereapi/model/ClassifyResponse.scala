package io.cequence.cohereapi.model

import java.util.UUID

case class ClassifyResponse(
  id: String,
  classifications: Seq[ClassifyResult],
  meta: ResponseMeta
)

case class ClassifyResult(
  id: UUID,
  classification_type: String,
  confidence: Double,
  confidences: Seq[Double],
  input: String,
  labels: Map[String, LabelConfidence],
  prediction: String,
  predictions: Seq[String]
)

case class LabelConfidence(
  confidence: Double
)
