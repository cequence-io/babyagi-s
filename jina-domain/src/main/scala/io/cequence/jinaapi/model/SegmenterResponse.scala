package io.cequence.jinaapi.model

case class SegmenterResponse(
  num_tokens: Int,
  tokenizer: String,
  usage: Usage,
  chunk_positions: Seq[Seq[Any]],
  chunks: Seq[String]
)

case class SegmenterData(
  data: String
)
