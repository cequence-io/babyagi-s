package io.cequence.cohereapi.model

import io.cequence.wsclient.domain.EnumValue

case class ChatSettings(
  /**
    * The name of a compatible Cohere model or the ID of a fine-tuned model.
    * 
    * Compatible Deployments: Cohere Platform, Private Deployments
    */
  model: String,

  /**
   * When specified, the default Cohere preamble will be replaced with the provided one.
   * Preambles are a part of the prompt used to adjust the model's overall behavior and conversation style,
   * and use the SYSTEM role.
   *
   * The SYSTEM role is also used for the contents of the optional chat_history= parameter.
   * When used with the chat_history= parameter it adds content throughout a conversation.
   * Conversely, when used with the preamble= parameter it adds content at the start of the conversation only.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  preamble: Option[String] = None,

  /**
   * A list of previous messages in the conversation, allowing the model to have context of the ongoing dialogue.
   * Each message should be a tuple of (role, content), where role is either "USER" or "CHATBOT".
   * The chat_history parameter uses the SYSTEM role when sent to the model.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  chat_history: Seq[ChatMessage] = Nil,

  /**
   * When true, the response will be a JSON stream of events. The final event will contain
   * the complete response, and will have an event_type of "stream-end".
   * Streaming is beneficial for user interfaces that render the contents of the response
   * piece by piece, as it gets generated.
   * Defaults to false.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  stream: Boolean = false,

  /**
   * An alternative to chat_history.
   * Providing a conversation_id creates or resumes a persisted conversation with the specified ID.
   * The ID can be any non-empty string.
   *
   * Compatible Deployments: Cohere Platform
   */
  conversation_id: Option[String] = None,

  /**
   * Dictates how the prompt will be constructed.
   * Defaults to AUTO when connectors are specified and OFF in all other cases.
   *
   * - AUTO: Some elements from chat_history and documents will be dropped to fit within the model's context length limit.
   *         The order of documents and chat history may change and be ranked by relevance.
   * - AUTO_PRESERVE_ORDER: Similar to AUTO, but preserves the input order of documents and chat history.
   * - OFF: No elements will be dropped. If inputs exceed the model's context length limit, a TooManyTokens error will be returned.
   *
   * Compatible Deployments:
   * - AUTO: Cohere Platform Only
   * - AUTO_PRESERVE_ORDER: Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  prompt_truncation: Option[PromptTruncation] = None,

  /**
   * A list of connectors to enrich the model's reply with information.
   * Accepts {"id": "web-search"}, and/or the "id" for a custom connector, if you've created one.
   * When specified, the model's reply will be enriched with information found by querying each of the connectors (RAG).
   *
   * Compatible Deployments: Cohere Platform
   */
  connectors: Seq[Connector] = Nil,

  /**
   * When true, the response will only contain a list of generated search queries, but no search will take place,
   * and no reply from the model to the user's message will be generated.
   * Defaults to false.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  search_queries_only: Boolean = false,

  /**
   * A list of relevant documents that the model can cite to generate a more accurate reply.
   * Each document is a string-string dictionary.
   *
   * Example:
   * [
   *   { "title": "Tall penguins", "text": "Emperor penguins are the tallest." },
   *   { "title": "Penguin habitats", "text": "Emperor penguins only live in Antarctica." },
   * ]
   *
   * Keys and values from each document will be serialized to a string and passed to the model.
   * The resulting generation will include citations that reference some of these documents.
   *
   * Some suggested keys are "text", "author", and "date". For better generation quality,
   * it is recommended to keep the total word count of the strings in the dictionary to under 300 words.
   *
   * An id field (string) can be optionally supplied to identify the document in the citations.
   * This field will not be passed to the model.
   *
   * An _excludes field (array of strings) can be optionally supplied to omit some key-value pairs
   * from being shown to the model. The omitted fields will still show up in the citation object.
   * The "_excludes" field will not be passed to the model.
   *
   * See 'Document Mode' in the guide for more information.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  documents: Seq[Map[String, String]] = Nil,

  /**
   * Dictates the approach taken to generating citations as part of the RAG flow by allowing
   * the user to specify whether they want "accurate" results, "fast" results or no results.
   * Defaults to "accurate".
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  citation_quality: Option[CitationQuality] = None,

  /**
   * A non-negative float that tunes the degree of randomness in generation.
   * Lower temperatures mean less random generations, and higher temperatures mean more random generations.
   * Defaults to 0.3.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  temperature: Option[Double] = None,

  /**
   * The maximum number of tokens the model will generate as part of the response.
   * Note: Setting a low value may result in incomplete generations.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  max_tokens: Option[Int] = None,

  /**
   * The maximum number of input tokens to send to the model.
   * If not specified, max_input_tokens is the model's context length limit minus a small buffer.
   * Input will be truncated according to the prompt_truncation parameter.
   *
   * Compatible Deployments: Cohere Platform
   */
  max_input_tokens: Option[Int] = None,

  /**
   * Ensures only the top k most likely tokens are considered for generation at each step.
   * Defaults to 0, min value of 0, max value of 500.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  k: Option[Int] = None,

  /**
   * Ensures that only the most likely tokens, with total probability mass of p,
   * are considered for generation at each step. If both k and p are enabled, p acts after k.
   * Defaults to 0.75. min value of 0.01, max value of 0.99.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  p: Option[Double] = None,

  /**
   * If specified, the backend will make a best effort to sample tokens deterministically,
   * such that repeated requests with the same seed and parameters should return the same result.
   * However, determinism cannot be totally guaranteed.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  seed: Option[Int] = None,

  /**
   * A list of up to 5 strings that the model will use to stop generation.
   * If the model generates a string that matches any of the strings in the list,
   * it will stop generating tokens and return the generated text up to that point
   * not including the stop sequence.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  stop_sequences: Seq[String] = Nil,

  /**
   * Used to reduce repetitiveness of generated tokens.
   * The higher the value, the stronger a penalty is applied to previously present tokens,
   * proportional to how many times they have already appeared in the prompt or prior generation.
   * Defaults to 0.0, min value of 0.0, max value of 1.0.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  frequency_penalty: Option[Double] = Some(0.0),

  /**
   * Used to reduce repetitiveness of generated tokens.
   * Similar to frequency_penalty, except that this penalty is applied equally to all tokens
   * that have already appeared, regardless of their exact frequencies.
   * Defaults to 0.0, min value of 0.0, max value of 1.0.
   *
   * Compatible Deployments: Cohere Platform, Azure, AWS Sagemaker/Bedrock, Private Deployments
   */
  presence_penalty: Option[Double] = Some(0.0),

  /**
   * Configuration for forcing the model output to adhere to the specified format.
   * Supported on Command R 03-2024, Command R+ 04-2024 and newer models.
   *
   * Compatible Deployments: Cohere Platform
   * 
   * The model can be forced into outputting JSON objects (with up to 5 levels of nesting) by setting { "type": "json_object" }.
   * 
   * A JSON Schema can optionally be provided, to ensure a specific structure.
   */
  response_type: Option[ResponseType] = None

//   tools: Option[String] = None,
//   tool_results: Option[String] = None,
//   force_single_step: Option[Int] = None,
//   safety_mode: Option[Seq[String]] = None,
//   returnLikelihoods: Option[String] = None,
)

sealed trait ResponseType

object ResponseType {
  case object Text extends ResponseType
  case class JsonObject(schema: Option[Map[String, Any]] = None) extends ResponseType
}

sealed trait PromptTruncation extends EnumValue

case object PromptTruncation {

  case object AUTO extends PromptTruncation
  case object AUTO_PRESERVE_ORDER extends PromptTruncation
  case object OFF extends PromptTruncation
}

sealed trait CitationQuality extends EnumValue

object CitationQuality {
  case object accurate extends CitationQuality
  case object fast extends CitationQuality
  case object off extends CitationQuality
}