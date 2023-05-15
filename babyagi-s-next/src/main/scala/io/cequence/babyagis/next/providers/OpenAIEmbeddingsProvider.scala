package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.openaiscala.domain.settings.CreateEmbeddingsSettings
import io.cequence.openaiscala.service.OpenAIServiceFactory

import scala.concurrent.{ExecutionContext, Future}

private class OpenAIEmbeddingsProvider(
  modelId: String,
  config: Config)(
  implicit ec: ExecutionContext, materializer: Materializer
) extends EmbeddingsProvider with OpenAIHelper {

  private val openAIService = OpenAIServiceFactory(config)

  override def apply(
    input: Seq[String]
  ): Future[Seq[Seq[Double]]] = retryAux(
    openAIService.createEmbeddings(
      input = input,
      settings = CreateEmbeddingsSettings(modelId)
    ).map(
      _.data.map(_.embedding)
    )
  )

  override def modelName: String = s"OpenAI: ${modelId}"
}

object OpenAIEmbeddingsProvider {
  def apply(
    modelName: String,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): EmbeddingsProvider =
    new OpenAIEmbeddingsProvider(modelName, config)
}