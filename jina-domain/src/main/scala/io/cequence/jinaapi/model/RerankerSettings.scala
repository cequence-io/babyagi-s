package io.cequence.jinaapi.model

case class RerankerSettings(
  // reranker model... see RerankerId
  model: String,

  // The number of most relevant documents to return for the query.
  top_n: Int
)