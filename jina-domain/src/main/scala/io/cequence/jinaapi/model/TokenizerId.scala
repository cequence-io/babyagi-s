package io.cequence.jinaapi.model

object TokenizerId {
  // Used in gpt-4, gpt-3.5-turbo, gpt-3.5, gpt-35-turbo, davinci-002, babbage-002, text-embedding-ada-002, text-embedding-3-small, text-embedding-3-large.
  val cl100k_base = "cl100k_base"

  // Used in gpt-4o, gpt-4o-mini.
  val o200k_base = "o200k_base"

  // Used in text-davinci-003, text-davinci-002, code-davinci-002, code-davinci-001, code-cushman-002, code-cushman-001, davinci-codex, cushman-codex.
  val p50k_base = "p50k_base"

  // Used in text-davinci-001, text-curie-001, text-babbage-001, text-ada-001, davinci, curie, babbage, ada, text-similarity-davinci-001, text-similarity-curie-001, text-similarity-babbage-001, text-similarity-ada-001, text-search-davinci-doc-001, text-search-curie-doc-001, text-search-babbage-doc-001, text-search-ada-doc-001, code-search-babbage-code-001, code-search-ada-code-001.
  val r50k_base = "r50k_base"

  // Used in text-davinci-edit-001, code-davinci-edit-001.
  val p50k_edit = "p50k_edit"

  // Used in gpt2, gpt-2.
  val gpt2 = "gpt2"
}
