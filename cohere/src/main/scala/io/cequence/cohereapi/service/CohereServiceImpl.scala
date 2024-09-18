package io.cequence.cohereapi.service

import akka.stream.Materializer
import io.cequence.cohereapi.{CohereClientTimeoutException, CohereClientUnknownHostException}
import io.cequence.cohereapi.model._
import io.cequence.cohereapi.JsonFormats._
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.WsRequestContext
import io.cequence.wsclient.service.WSClientEngine
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.ws.PlayWSClientEngine
import org.slf4j.LoggerFactory

import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future}
import io.cequence.cohereapi.JsonFormats

private class CohereServiceImpl(
  apiKey: String,
  clientName: Option[String]
)(
  implicit val ec: ExecutionContext,
  val materializer: Materializer
) extends CohereService
    with WSClientWithEngine {

  override protected type PEP = String
  override protected type PT = String

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override protected val engine: WSClientEngine = PlayWSClientEngine(
    coreUrl = "https://api.cohere.com/v1",
    requestContext = WsRequestContext(
      authHeaders = Seq(("Authorization", s"Bearer $apiKey"))
        ++ clientName.map("X-Client-Name" -> _)
    ),
    recoverErrors = { (serviceEndPointName: String) =>
      {
        case e: TimeoutException =>
          throw new CohereClientTimeoutException(
            s"${serviceEndPointName} timed out: ${e.getMessage}."
          )
        case e: UnknownHostException =>
          throw new CohereClientUnknownHostException(
            s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
          )
      }
    }
  )

  object Endpoint {
    val embed = "embed"
    val rerank = "rerank"
    val classify = "classify"
    val chat = "chat"
  }

  object Param {
    val query = "query"
    val documents = "documents"
    val model = "model"
    val top_n = "top_n"
    val return_documents = "return_documents"
    val rank_fields = "rank_fields"
    val max_chunks_per_doc = "max_chunks_per_doc"
    val inputs = "inputs"
    val examples = "examples"
    val preset = "preset"
    val truncate = "truncate"
    val texts = "texts"
    val input_type = "input_type"
    val embedding_types = "embedding_types"
    val message = "message"
    val user_id = "user_id"
    val session_id = "session_id"
    val temperature = "temperature"
    val max_tokens = "max_tokens"
    val stop_sequences = "stop_sequences"
    val return_likelihoods = "return_likelihoods"
    val search_queries_only = "search_queries_only"
    val chat_history = "chat_history"
    val conversation_id = "conversation_id"
    val prompt_truncation = "prompt_truncation"
    val connectors = "connectors"
    val citation_quality = "citation_quality"
    val preamble = "preamble"
    val stream = "stream"
    val max_input_tokens = "max_input_tokens"
    val k = "k"
    val p = "p"
    val seed = "seed"
    val frequency_penalty = "frequency_penalty"
    val presence_penalty = "presence_penalty"
    val response_type = "response_type"
  }

  override def embed(
    texts: Seq[String],
    settings: EmbedSettings
  ): Future[EmbedResponse] = {
    assert(texts.nonEmpty, "texts must not be empty")

    execPOST(
      Endpoint.embed,
      bodyParams = jsonBodyParams(
        Param.texts -> Some(texts),
        Param.model -> Some(settings.model),
        Param.input_type -> settings.input_type.map(_.toString()),
        Param.embedding_types -> (
          if (settings.embedding_types.nonEmpty)
            Some(settings.embedding_types.map(_.toString()))
          else None
        ),
        Param.truncate -> settings.truncate.map(_.toString())
      )
    ).map(
      _.asSafeJson[EmbedResponse]
    )
  }

  override def rerank(
    query: String,
    documents: Seq[Map[String, Any]],
    settings: RerankSettings
  ): Future[RerankResponse] =
    execPOST(
      Endpoint.rerank,
      bodyParams = jsonBodyParams(
        Param.query -> Some(query),
        Param.documents -> Some(documents),
        Param.model -> Some(settings.model),
        Param.top_n -> settings.top_n,
        Param.return_documents -> settings.return_documents,
        Param.rank_fields -> (
          if (settings.rank_fields.nonEmpty) Some(settings.rank_fields) else None
        ),
        Param.max_chunks_per_doc -> settings.max_chunks_per_doc
      )
    ).map(
      _.asSafeJson[RerankResponse]
    )

  override def classify(
    inputs: Seq[String],
    examples: Seq[(String, String)],
    settings: ClassifySettings
  ): Future[ClassifyResponse] =
    execPOST(
      Endpoint.classify,
      bodyParams = jsonBodyParams(
        Param.inputs -> Some(inputs),
        Param.examples -> Some(examples.map { case (text, label) =>
          Map("text" -> text, "label" -> label)
        }),
        Param.model -> Some(settings.model),
        Param.preset -> settings.preset,
        Param.truncate -> settings.truncate.map(_.toString())
      )
    ).map(
      _.asSafeJson[ClassifyResponse]
    )
 
  override def chat(
    message: String,
    settings: ChatSettings
  ): Future[ChatResponse] = {
    execPOST(
      Endpoint.chat,
      bodyParams = jsonBodyParams(
        Param.model -> Some(settings.model),
        Param.message -> Some(message),
        Param.preamble -> settings.preamble,
        Param.chat_history -> (
          if (settings.chat_history.nonEmpty)
            Some(settings.chat_history.map(JsonFormats.chatMessageFormat.writes))
          else None
        ),
        Param.stream -> Some(settings.stream),
        Param.conversation_id -> settings.conversation_id,
        Param.prompt_truncation -> settings.prompt_truncation.map(_.toString),
        Param.search_queries_only -> Some(settings.search_queries_only),
        Param.citation_quality -> settings.citation_quality.map(_.toString),
        Param.connectors -> (if (settings.connectors.nonEmpty) Some(settings.connectors.map(JsonFormats.connectorFormat.writes)) else None),
        Param.documents -> (if (settings.documents.nonEmpty) Some(settings.documents) else None),
        Param.temperature -> settings.temperature,
        Param.max_tokens -> settings.max_tokens,
        Param.max_input_tokens -> settings.max_input_tokens,
        Param.k -> settings.k,
        Param.p -> settings.p,
        Param.seed -> settings.seed,
        Param.stop_sequences -> (if (settings.stop_sequences.nonEmpty) Some(settings.stop_sequences) else None),
        Param.frequency_penalty -> settings.frequency_penalty,
        Param.presence_penalty -> settings.presence_penalty,
        Param.response_type -> settings.response_type.map(JsonFormats.responseTypeFormat.writes)
      )
    ).map(
      _.asSafeJson[ChatResponse]
    )
  }
}

object CohereServiceFactory {

  private val envAPIKey = "COHERE_API_KEY"

  def apply(
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): CohereService =
    apply(getAPIKeyFromEnv(), None)

  def apply(
    apiKey: String,
    clientName: Option[String] = None
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): CohereService =
    new CohereServiceImpl(apiKey, clientName)

  private def getAPIKeyFromEnv(): String =
    Option(System.getenv(envAPIKey)).getOrElse(
      throw new IllegalStateException(
        "COHERE_API_KEY environment variable expected but not set. Alternatively, you can pass the API key explicitly to the factory method."
      )
    )
}
