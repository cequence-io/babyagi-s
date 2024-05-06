package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.pineconescala.domain.settings.{CreatePodBasedIndexSettings, QuerySettings}
import io.cequence.pineconescala.domain.{Metric, PVector, PodType}
import io.cequence.pineconescala.service.{PineconeIndexService, PineconeIndexServiceFactory, PineconeVectorService, PineconeVectorServiceFactory}

import scala.concurrent.{ExecutionContext, Future}

private class PineconeVectorStoreProvider(
  pineconeVectorService: PineconeVectorService,
  namespace: String)(
  implicit ec: ExecutionContext
) extends VectorStoreProvider with PineconeHelper {

  override def add(
    id: String,
    vector: Seq[Double],
    metadata: Map[String, String]
  ): Future[Unit] = retryAux(
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
  )

  override def querySorted(
    vector: Seq[Double],
    topResultsNum: Int,
    metadataFieldName: String
  ): Future[Seq[String]] = retryAux(
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
  )
}

object PineconeVectorStoreProvider extends PineconeHelper {

  def apply(
    indexName: String,
    namespace: String,
    dimension: Int,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): Future[VectorStoreProvider] = {
    val pineconeIndexService = PineconeIndexServiceFactory(config)

    createVectorServiceAux(pineconeIndexService, indexName, dimension, config).map {
      new PineconeVectorStoreProvider(_, namespace)
    }
  }

  private def createVectorServiceAux(
    pineconeIndexService: PineconeIndexService,
    indexName: String,
    dimension: Int,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): Future[PineconeVectorService] =
    for {
      indexNames <- pineconeIndexService.listIndexes

      _ <- if (!indexNames.contains(indexName)) {
        retryAux(
          pineconeIndexService.createIndex(
            indexName,
            dimension,
            settings = CreatePodBasedIndexSettings(
              metric = Metric.cosine,
              pods = 1,
              replicas = 1,
              podType = PodType.p1_x1
            )
          )
        )
      } else {
        Future()
      }

      vectorService <- retryAux(
        PineconeVectorServiceFactory(
          indexName,
          config
        ).map(_.getOrElse(
          throw new Exception(s"Could not find index '${indexName}'")
        ))
      )

      indexStatsResponse <- retryAux(vectorService.describeIndexStats)
    } yield {
      assert(dimension == indexStatsResponse.dimension, "Dimension of the index does not match the dimension of the LLM embedding")

      vectorService
    }
}