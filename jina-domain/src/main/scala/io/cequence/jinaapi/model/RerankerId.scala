package io.cequence.jinaapi.model

object RerankerId {

  // The best multilingual ColBERT with top performance on embedding and reranking
  // Active, 8192, 2024-08-12
  val jina_colbert_v2 = "jina-colbert-v2"

  // The latest and best reranker model with multilingual, function calling and code search support.
  // Active, 8192, 2024-06-20
  val jina_reranker_v2_base_multilingual = "jina-reranker-v2-base-multilingual"

  // The best combination of fast inference speed and accurate relevance scores
  // Active, 8192, 2024-04-18
  val jina_reranker_v1_turbo_en = "jina-reranker-v1-turbo-en"

  // The fastest reranker model, best suited for ranking a large number of documents reliably
  // Inactive, 8192, 2024-04-18
  val jina_reranker_v1_tiny_en = "jina-reranker-v1-tiny-en"

  // Our first reranker model maximizing search and RAG relevance
  // Active, 8192, 2024-02-29
  val jina_reranker_v1_base_en = "jina-reranker-v1-base-en"

  // Improved ColBERT with 8K-token length for embedding and reranking tasks
  // Active, 8192, 2024-02-17
  val jina_colbert_v1_en = "jina-colbert-v1-en"
}
