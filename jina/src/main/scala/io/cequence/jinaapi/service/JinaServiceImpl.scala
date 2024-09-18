package io.cequence.jinaapi.service

import akka.stream.Materializer
import io.cequence.jinaapi.{JinaClientTimeoutException, JinaClientUnknownHostException}
import io.cequence.jinaapi.model._
import io.cequence.jinaapi.JsonFormats._
import io.cequence.wsclient.ResponseImplicits.JsonSafeOps
import io.cequence.wsclient.domain.WsRequestContext
import io.cequence.wsclient.service.WSClientEngine
import io.cequence.wsclient.service.WSClientWithEngineTypes.WSClientWithEngine
import io.cequence.wsclient.service.ws.PlayWSClientEngine
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future}

private class JinaServiceImpl(
  apiKey: String
)(
  implicit val ec: ExecutionContext,
  val materializer: Materializer
) extends JinaService
    with WSClientWithEngine {

  override protected type PEP = String
  override protected type PT = String

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override protected val engine: WSClientEngine = PlayWSClientEngine(
    coreUrl = "https://",
    requestContext = WsRequestContext(
      authHeaders = Seq(("Authorization", s"Bearer $apiKey"))
    ),
    recoverErrors = { (serviceEndPointName: String) =>
      {
        case e: TimeoutException =>
          throw new JinaClientTimeoutException(
            s"${serviceEndPointName} timed out: ${e.getMessage}."
          )
        case e: UnknownHostException =>
          throw new JinaClientUnknownHostException(
            s"${serviceEndPointName} cannot resolve a host name: ${e.getMessage}."
          )
      }
    }
  )

  object Endpoint {
    val crawl = "r.jina.ai"
    val search = "s.jina.ai"
    val segment = "segment.jina.ai"
    val rerank = "api.jina.ai/v1/rerank"
  }

  object Param {
    val accept = "Accept"
    val locale = "X-Locale"
    val noCache = "X-No-Cache"
    val proxyUrl = "X-Proxy-Url"
    val returnFormat = "X-Return-Format"
    val setCookie = "X-Set-Cookie"
    val targetSelector = "X-Target-Selector"
    val timeout = "X-Timeout"
    val waitForSelector = "X-Wait-For-Selector"
    val url = "url"
    val content = "content"
    val return_tokens = "return_tokens"
    val return_chunks = "return_chunks"
    val max_chunk_length = "max_chunk_length"
    val tokenizer = "tokenizer"
    val head = "head"
    val tail = "tail"
    val query = "query"
    val documents = "documents"
    val model = "model"
    val top_n = "top_n"
  }

  override def crawl(
    url: String,
    settings: CrawlerSettings
  ): Future[CrawlResponse] =
    execPOST(
      Endpoint.crawl,
      bodyParams = jsonBodyParams(
        params = Param.url -> Some(url)
      ),
      extraHeaders = crawlHeaders(settings, useJsonResponse = true)
    ).map(
      _.asSafeJson[CrawlResponse]
    )

  override def crawlString(
    url: String,
    settings: CrawlerSettings
  ): Future[String] =
    execPOST(
      Endpoint.crawl,
      bodyParams = jsonBodyParams(
        params = Param.url -> Some(url)
      ),
      extraHeaders = crawlHeaders(settings, useJsonResponse = false)
    ).map(
      _.string
    )

  private def crawlHeaders(
    settings: CrawlerSettings,
    useJsonResponse: Boolean
  ) = Seq(
    Param.accept -> (if (useJsonResponse) Some("application/json") else None),
    Param.locale -> settings.browserLocale,
    Param.noCache -> settings.noCache.map(_.toString),
    Param.proxyUrl -> settings.proxyURL,
    Param.returnFormat -> settings.contentFormat.map(_.toString()),
    Param.setCookie -> settings.forwardCookie,
    Param.targetSelector -> settings.targetSelector,
    Param.timeout -> settings.timeout.map(_.toString),
    Param.waitForSelector -> settings.waitForSelector
  ).collect { case (key, Some(value)) => (key, value) }

  override def segment(
    content: String,
    settings: SegmenterSettings
  ): Future[SegmenterResponse] = {
    // either head or tail can be set, but not both
    assert(
      settings.head.isEmpty || settings.tail.isEmpty,
      "Either head or tail can be set, but not both."
    )

    execPOST(
      Endpoint.segment,
      bodyParams = jsonBodyParams(
        Param.content -> Some(content),
        Param.return_tokens -> settings.returnTokens,
        Param.return_chunks -> settings.returnChunks,
        Param.max_chunk_length -> settings.maxChunkLength,
        Param.head -> settings.head,
        Param.tail -> settings.tail,
        Param.tokenizer -> settings.tokenizer
      )
    ).map { response =>
      println(Json.prettyPrint(response.json))
      response.asSafeJson[SegmenterResponse]
    }
  }

  override def rerank(
    query: String,
    documents: Seq[String],
    settings: RerankerSettings
  ): Future[RerankerResponse] =
    execPOST(
      Endpoint.rerank,
      bodyParams = jsonBodyParams(
        Param.query -> Some(query),
        Param.documents -> Some(documents),
        Param.model -> Some(settings.model),
        Param.top_n -> Some(settings.top_n)
      )
    ).map(
      _.asSafeJson[RerankerResponse]
    )
}

object JinaServiceFactory {

  private val envAPIKey = "JINA_API_KEY"

  def apply(
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): JinaService =
    apply(getAPIKeyFromEnv())

  def apply(
    apiKey: String
  )(
    implicit ec: ExecutionContext,
    materializer: Materializer
  ): JinaService =
    new JinaServiceImpl(apiKey)

  private def getAPIKeyFromEnv(): String =
    Option(System.getenv(envAPIKey)).getOrElse(
      throw new IllegalStateException(
        s"${envAPIKey} environment variable expected but not set. Alternatively, you can pass the API key explicitly to the factory method."
      )
    )
}
