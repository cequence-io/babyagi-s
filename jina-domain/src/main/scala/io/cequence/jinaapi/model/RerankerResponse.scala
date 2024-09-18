package io.cequence.jinaapi.model

case class RerankerResponse(
  model: String,
  results: Seq[RerankerResult],
  usage: RerankerUsage
)

case class RerankerResult(
  index: Int,
  document: RerankerDocument,
  relevance_score: Double
)

case class RerankerDocument(
  text: String
)

case class RerankerUsage(
  total_tokens: Int,
  prompt_tokens: Int
)
