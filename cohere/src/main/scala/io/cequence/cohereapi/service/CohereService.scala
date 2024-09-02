package io.cequence.cohereapi.service

import io.cequence.cohereapi.model._
import io.cequence.cohereapi.model.RerankSettings
import io.cequence.wsclient.service.CloseableService

import scala.concurrent.Future

trait CohereService extends CohereConsts with CloseableService {

  /**
   * This endpoint returns text embeddings. An embedding is a list of floating point numbers
   * that captures semantic information about the text that it represents.
   *
   * Embeddings can be used to create text classifiers as well as empower semantic search. To
   * learn more about embeddings, see the embedding page.
   *
   * @param texts
   *   An array of strings for the model to embed. Maximum number of texts per call is 96. We
   *   recommend reducing the length of each text to be under 512 tokens for optimal quality.
   * @param settings
   * @return
   *
   * @see
   *   <a href="https://docs.cohere.com/reference/embed>Cohere API Doc</a>
   */
  def embed(
    texts: Seq[String],
    settings: EmbedSettings = Defaults.Embed
  ): Future[EmbedResponse]

  /**
   * This endpoint takes in a query and a list of texts and produces an ordered array with each
   * text assigned a relevance score.
   *
   * @param query
   *   The search query
   * @param documents
   *   A list of document objects or strings to rerank. If a document is provided the text
   *   fields is required and all other fields will be preserved in the response. The total max
   *   chunks (length of documents * max_chunks_per_doc) must be less than 10000. We recommend
   *   a maximum of 1,000 documents for optimal endpoint performance.
   * @param settings
   * @return
   *
   * @see
   *   <a href="https://docs.cohere.com/reference/rerank">Cohere API Doc</a>
   */
  def rerank(
    query: String,
    documents: Seq[Map[String, Any]],
    settings: RerankSettings = Defaults.Rerank
  ): Future[RerankResponse]

  /**
   * This endpoint makes a prediction about which label fits the specified text inputs best. To
   * make a prediction, Classify uses the provided examples of text + label pairs as a
   * reference. Note: Fine-tuned models trained on classification examples don’t require the
   * examples parameter to be passed in explicitly.
   *
   * @param inputs
   *   A list of up to 96 texts to be classified. Each one must be a non-empty string. There
   *   is, however, no consistent, universal limit to the length a particular input can be. We
   *   perform classification on the first x tokens of each input, and x varies depending on
   *   which underlying model is powering classification. The maximum token length for each
   *   model is listed in the “max tokens” column here. Note: by default the truncate parameter
   *   is set to END, so tokens exceeding the limit will be automatically dropped. This
   *   behavior can be disabled by setting truncate to NONE, which will result in validation
   *   errors for longer texts.
   * @param examples
   *   An array of examples to provide context to the model. Each example is a text string and
   *   its associated label/class. Each unique label requires at least 2 examples associated
   *   with it; the maximum number of examples is 2500, and each example has a maximum length
   *   of 512 tokens. The values should be structured as {text: "...",label: "..."}. Note:
   *   Fine-tuned Models trained on classification examples don’t require the examples
   *   parameter to be passed in explicitly.
   * @param settings
   * @return
   *
   * @see
   *   <a href="https://docs.cohere.com/reference/classify">Cohere API Doc</a>
   */
  def classify(
    inputs: Seq[String],
    examples: Seq[(String, String)],
    settings: ClassifySettings = Defaults.Classify
  ): Future[ClassifyResponse]
}
