package io.cequence.babyagis.next.domain

sealed trait EmbeddingsSettings

case class ONNXEmbeddingsSettings(
  tokenizerPath: String,
  modelPath: String,
  normalize: Boolean,
  modelDisplayName: String
) extends EmbeddingsSettings

case class OpenAIEmbeddingsSettings(
  modelName: String
) extends EmbeddingsSettings