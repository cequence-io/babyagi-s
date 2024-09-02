package io.cequence.cohereapi.model

case class ClassifySettings(
  // The identifier of the model.
  // Smaller “light” models are faster, while larger models will perform better.
  // Fine-tuned models can also be supplied with their full ID.
  model: String,

  // The ID of a custom playground preset. You can create presets in the playground. If you use a preset,
  // all other parameters become optional, and any included parameters will override the preset’s parameters.
  preset: Option[String] = None,

  // One of NONE|START|END to specify how the API will handle inputs longer than the maximum token length.
  // Passing START will discard the start of the input. END will discard the end of the input.
  // In both cases, input is discarded until the remaining input is exactly the maximum input token length for the model.
  // If NONE is selected, when the input exceeds the maximum input token length an error will be returned.
  // Defaults to END
  truncate: Option[TruncateType] = None
)
