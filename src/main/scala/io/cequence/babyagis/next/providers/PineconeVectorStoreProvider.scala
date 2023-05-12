package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.pineconescala.domain.settings.{CreateIndexSettings, QuerySettings}
import io.cequence.pineconescala.domain.{Metric, PVector, PodType}
import io.cequence.pineconescala.service.{PineconeIndexService, PineconeIndexServiceFactory, PineconeVectorService, PineconeVectorServiceFactory}

import scala.concurrent.{ExecutionContext, Future}

private class PineconeVectorStoreProvider(
  pineconeVectorService: PineconeVectorService,
  namespace: String)(
  implicit ec: ExecutionContext
) extends VectorStoreProvider {

  override def add(
    id: String,
    vector: Seq[Double],
    metadata: Map[String, String]
  ): Future[Unit] =
    pineconeVectorService.upsert(
      vectors = Seq(
        PVector(
          id,
          vector,
          sparseValues = None,
          metadata = metadata
        )
      ),
      namespace
    ).map(_ => ())

  override def querySorted(
    vector: Seq[Double],
    topResultsNum: Int,
    metadataFieldName: String
  ): Future[Seq[String]] =
    for {
      results <- pineconeVectorService.query(
        vector,
        namespace = namespace,
        settings = QuerySettings(
          topK = topResultsNum,
          includeMetadata = true,
          includeValues = false
        )
      )
    } yield {
      val sortedResults = results.matches.sortBy(-_.score)

      sortedResults.flatMap(_.metadata.map(_(metadataFieldName)))
    }
}

object PineconeVectorStoreProvider {

  private val dimension = 1536

  def apply(
    indexName: String,
    namespace: String,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): Future[VectorStoreProvider] = {
    val pineconeIndexService = PineconeIndexServiceFactory(config)

    createVectorServiceAux(pineconeIndexService, indexName, config).map {
      new PineconeVectorStoreProvider(_, namespace)
    }
  }

  private def createVectorServiceAux(
    pineconeIndexService: PineconeIndexService,
    indexName: String,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): Future[PineconeVectorService] =
    for {
      indexNames <- pineconeIndexService.listIndexes

      _ <- if (!indexNames.contains(indexName)) {
        pineconeIndexService.createIndex(
          indexName,
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
        indexName,
        config
      ).map(_.getOrElse(
        throw new Exception(s"Could not find index '${indexName}'")
      ))

      indexStatsResponse <- vectorService.describeIndexStats
    } yield {
      assert(dimension == indexStatsResponse.dimension, "Dimension of the index does not match the dimension of the LLM embedding")

      vectorService
    }
}