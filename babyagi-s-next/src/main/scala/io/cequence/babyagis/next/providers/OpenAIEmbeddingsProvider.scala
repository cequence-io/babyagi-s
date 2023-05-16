package io.cequence.babyagis.next.providers

import java.{util => ju}
import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.openaiscala.domain.settings.CreateEmbeddingsSettings
import io.cequence.openaiscala.service.OpenAIServiceFactory
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private class OpenAIEmbeddingsProvider(
  modelId: String,
  config: Config)(
  implicit ec: ExecutionContext, materializer: Materializer
) extends EmbeddingsProvider with OpenAIHelper {

  private val openAIService = OpenAIServiceFactory(config)
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def apply(
    input: Seq[String]
  ): Future[Seq[Seq[Double]]] = {
    val start = new ju.Date()

    retryAux(
      openAIService.createEmbeddings(
        input = input,
        settings = CreateEmbeddingsSettings(modelId)
      ).map(
        _.data.map(_.embedding)
      )
    ).map { embeddings =>
      logger.info(s"OpenAI-based embedding with the model '${modelId}' took ${new ju.Date().getTime - start.getTime} ms for ${input.size} inputs with ${input.map(_.length).sum} characters in total.")
      embeddings
    }
  }

  override def modelName: String = s"OpenAI: $modelId"
}

object OpenAIEmbeddingsProvider {
  def apply(
    modelName: String,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): EmbeddingsProvider =
    new OpenAIEmbeddingsProvider(modelName, config)
}