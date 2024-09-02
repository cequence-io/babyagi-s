package io.cequence.cohereapi.model

case class RerankSettings(
  // The identifier of the model to use, one of: rerank-english-v3.0, rerank-multilingual-v3.0, rerank-english-v2.0, rerank-multilingual-v2.0
  model: String,

  // The number of most relevant documents or indices to return, defaults to the length of the documents
  top_n: Option[Int] = None,

  // If a JSON object is provided, you can specify which keys you would like to have considered for reranking.
  // The model will rerank based on order of the fields passed in (i.e. rank_fields=[‘title’,‘author’,‘text’]
  // will rerank using the values in title, author, text sequentially.
  // If the length of title, author, and text exceeds the context length of the model,
  // the chunking will not re-consider earlier fields).
  // If not provided, the model will use the default text field for ranking.
  rank_fields: Seq[String] = Nil,

  // If false, returns results without the doc text; if true, returns results with the doc text passed in
  return_documents: Option[Boolean] = None,

  // The maximum number of chunks to produce internally from a document
  max_chunks_per_doc: Option[Int] = None
)
