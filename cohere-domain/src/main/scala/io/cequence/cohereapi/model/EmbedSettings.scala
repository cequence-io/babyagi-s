package io.cequence.cohereapi.model

import io.cequence.wsclient.domain.EnumValue

case class EmbedSettings(
  // The identifier of the model. Defaults to "embed-english-v2.0".
  // Smaller “light” models are faster, while larger models will perform better.
  // Custom models can also be supplied with their full ID.
  model: String,

  // Specifies the type of input passed to the model.
  // Required for embedding models v3 and higher.
  input_type: Option[InputType] = None,

  // Specifies the types of embeddings you want to get back.
  // Not required and default is None, which returns the Embed Floats response type.
  // Can be one or more of the following types.
  embedding_types: Seq[EmbeddingType] = Nil,

  //One of NONE|START|END to specify how the API will handle inputs longer than the maximum token length.
  // Defaults to END.
  truncate: Option[TruncateType] = None
)

sealed trait InputType extends EnumValue

object InputType {
  case object search_document extends InputType
  case object search_query extends InputType
  case object classification extends InputType
  case object clustering extends InputType
}

sealed trait EmbeddingType extends EnumValue

object EmbeddingType {
  case object float extends EmbeddingType
  case object `int8` extends EmbeddingType
  case object uint8 extends EmbeddingType
  case object binary extends EmbeddingType
  case object ubinary extends EmbeddingType
}
