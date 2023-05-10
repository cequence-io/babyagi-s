package io.cequence.babyagis.port

import akka.stream.Materializer
import io.cequence.openaiscala.domain.ModelId
import io.cequence.openaiscala.domain.settings.CreateEmbeddingsSettings
import io.cequence.openaiscala.service.OpenAIService
import io.cequence.pineconescala.service.{PineconeIndexServiceFactory, PineconeVectorService, PineconeVectorServiceFactory}
import io.cequence.pineconescala.domain.settings.{CreateIndexSettings, QuerySettings}
import io.cequence.pineconescala.domain.{Metric, PVector, PodType}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
class PineconeResultsStorage(
  pinecone_api_key: String,
  pinecone_environment: String,
  openAIService: OpenAIService,
  llm_model: String,
  results_store_name: String,
  objective: String)(
  implicit ec: ExecutionContext, materializer: Materializer
) {
  private val pineconeIndexService = PineconeIndexServiceFactory(pinecone_api_key, pinecone_environment)

  // Pinecone namespaces are only compatible with ascii characters (used in query and upsert)
  private val namespace = objective.replaceAll("[^\\x00-\\x7F]+", "")

  private val pineconeVectorService: PineconeVectorService = Await.result(init, 2 minutes)

  def init = {
    val dimension = if (!llm_model.startsWith("llama")) 1536 else 5120

    for {
      indexNames <- pineconeIndexService.listIndexes

      _ <- if (!indexNames.contains(results_store_name)) {
        pineconeIndexService.createIndex(
          results_store_name,
          dimension,
          settings = CreateIndexSettings(
            metric = Metric.cosine,
            pods = 1,
            replicas = 1,
            podType = PodType.p1_x1
          )
        )
      } else {
        Future()
      }

      vectorService <- PineconeVectorServiceFactory(
        pinecone_api_key,
        indexName = results_store_name,
        timeouts = None,
        pineconeIndexService
      ).map(_.getOrElse(
        throw new Exception(s"Could not find index '${results_store_name}'")
      ))

      index_stats_response <- vectorService.describeIndexStats
    } yield {
      assert(dimension == index_stats_response.dimension, "Dimension of the index does not match the dimension of the LLM embedding")

      vectorService
    }
  }

  type Dict = Map[String, Any]

  def add(
    task: Dict,
    result: String,
    result_id: String
  ) =
    for {
      vector <- get_embedding(result)

      _ <- pineconeVectorService.upsert(
        vectors = Seq(
          PVector(
            result_id,
            vector,
            sparseValues = None,
            metadata = Map(
              "task" -> task("task_name").toString,
              "result" -> result
            )
          )
        ),
        namespace
      )
    } yield
      ()


  def query(query: String, top_results_num: Int): Future[Seq[String]] =
    for {
      query_embedding <- get_embedding(query)

      results <- pineconeVectorService.query(
        query_embedding,
        namespace = namespace,
        settings = QuerySettings(
          topK = top_results_num,
          includeMetadata = true,
          includeValues = false
        )
      )
    } yield {
      val sortedResults = results.matches.sortBy(-_.score)

      sortedResults.flatMap(_.metadata.map(_("task")))
    }

  // Get embedding for the text
  def get_embedding(text: String): Future[Seq[Double]] = {
    val replacedText = text.replaceAll("\n", " ")

    if (llm_model.startsWith("llama")) {
      throw new IllegalArgumentException("Llama not supported yet")
    } else {
      openAIService.createEmbeddings(
        input = Seq(replacedText),
        settings = CreateEmbeddingsSettings(
          model = ModelId.text_embedding_ada_002
        )
      ).map(
        _.data.head.embedding
      )
    }
  }
}
