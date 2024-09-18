package io.cequence.cohereapi.model

object ChatModelId {
  // command-r-plus-08-2024 is an update of the Command R+ model, delivered in August 2024.
  val command_r_plus_08_2024 = "command-r-plus-08-2024"

  // Command R+ is an instruction-following conversational model for complex RAG workflows and multi-step tool use.
  val command_r_plus_04_2024 = "command-r-plus-04-2024"

  // command-r-plus is an alias for command-r-plus-04-2024
  val command_r_plus = "command-r-plus"

  // command-r-08-2024 is an update of the Command R model, delivered in August 2024.
  val command_r_08_2024 = "command-r-08-2024"

  // Command R is an instruction-following conversational model for complex workflows like code generation, RAG, tool use, and agents.
  val command_r_03_2024 = "command-r-03-2024"

  // command-r is an alias for command-r-03-2024
  val command_r = "command-r"

  // An instruction-following conversational model with high quality, reliability, and longer context than base generative models.
  val command = "command"

  // Nightly version of command model. Latest, experimental, and possibly unstable. Not recommended for production use.
  val command_nightly = "command-nightly"

  // A smaller, faster version of command. Almost as capable, but a lot faster.
  val command_light = "command-light"

  // Nightly version of command-light model. Latest, experimental, and possibly unstable. Not recommended for production use.
  val command_light_nightly = "command-light-nightly"
}
