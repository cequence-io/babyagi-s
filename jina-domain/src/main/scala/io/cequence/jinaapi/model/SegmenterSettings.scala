package io.cequence.jinaapi.model

case class SegmenterSettings(
  // Return the tokens and their corresponding ids in the response. Toggle to see the result visualization.
  returnTokens: Option[Boolean] = None,

  // Chunking the input into semantically meaningful segments while handling a wide variety of text types and edge cases based on common structural cues.
  returnChunks: Option[Boolean] = None,

  // Maximum number of characters in each chunk. In practice the chunk length can be smaller than this value, if there is a good boundary in the text.
  // The limit seems to be 2000
  maxChunkLength: Option[Int] = None,

  // Return the first N tokens of the given content. Boundary exclusive. Can not be used with 'tail'.
  head: Option[Int] = None,

  // Return the last N tokens of the given content. Boundary exclusive. Can not be used with 'head'.
  tail: Option[Int] = None,

  // The tokenizer to use.
  tokenizer: Option[String] = None
)