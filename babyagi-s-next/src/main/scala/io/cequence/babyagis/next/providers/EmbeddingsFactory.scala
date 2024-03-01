package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.babyagis.next.domain.{EmbeddingsSettings, ONNXEmbeddingsSettings, OpenAIEmbeddingsSettings, PineconeVectorStoreSettings, VectorStoreSettings}

import scala.concurrent.{ExecutionContext, Future}

object EmbeddingsFactory {

  def apply(
    embeddingsSettings: EmbeddingsSettings,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): EmbeddingsProvider =
    embeddingsSettings match {
      case settings: ONNXEmbeddingsSettings => ONNXEmbeddingsProvider(
        settings.tokenizerPath,
        settings.modelPath,
        settings.normalize,
        settings.modelDisplayName
      )

      case settings: OpenAIEmbeddingsSettings =>
        OpenAIEmbeddingsProvider(
          settings.modelName,
          config
        )
    }
}
