package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.babyagis.next.domain.{LocalVectorStoreSettings, PineconeVectorStoreSettings, VectorStoreSettings}

import scala.concurrent.{ExecutionContext, Future}

object VectorStoreFactory {

  def apply(
    storeSettings: VectorStoreSettings,
    dimension: Int,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): Future[VectorStoreProvider] =
    storeSettings match {
      case settings: PineconeVectorStoreSettings => PineconeVectorStoreProvider(
        settings.indexName,
        settings.namespace,
        dimension,
        config
      )

      case LocalVectorStoreSettings => Future(new DummyVectorStoreProvider())
    }
}
