package io.cequence.cohereapi.model

import java.util.UUID

case class EmbedResponse(
  id: UUID,
  response_type: String, // The type of response, e.g., "embeddings_floats"
  embeddings: Seq[Seq[Double]],
  texts: Seq[String],
  meta: ResponseMeta
)
