package io.cequence.cohereapi.model

import java.util.UUID

case class RerankResponse(
  id: UUID,
  results: Seq[RerankResult],
  meta: ResponseMeta
)

case class RerankResult(
  index: Int,
  document: Option[Map[String, Any]],
  relevance_score: Double
)
