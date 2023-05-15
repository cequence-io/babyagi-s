package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.openaiscala.OpenAIScalaClientException
import io.cequence.openaiscala.domain.settings.{CreateChatCompletionSettings, CreateCompletionSettings, CreateEmbeddingsSettings}
import io.cequence.openaiscala.domain.{ChatRole, MessageSpec}
import io.cequence.openaiscala.service.OpenAIServiceFactory

import scala.concurrent.{ExecutionContext, Future}

private class OpenAICompletionProvider(
  modelId: String,
  temperature: Double,
  config: Config)(
  implicit ec: ExecutionContext, materializer: Materializer
) extends CompletionProvider with OpenAIHelper {

  private val openAIService = OpenAIServiceFactory(config)

  override def apply(
    prompt: String,
    maxTokens: Int
  ): Future[String] = retryAux(
    if (!modelId.toLowerCase.startsWith("gpt-")) {
      // Use completion API
      openAIService.createCompletion(
        prompt = prompt,
        settings = CreateCompletionSettings(
          model = modelId,
          temperature = Some(temperature),
          max_tokens = Some(maxTokens)
        )
      ).map { response =>
        response.choices.head.text.strip()
      }
    } else {
      // Use chat completion API

      val messages = Seq(
        MessageSpec(ChatRole.System, prompt)
      )

      openAIService.createChatCompletion(
        messages = messages,
        settings = CreateChatCompletionSettings(
          model = modelId,
          temperature = Some(temperature),
          max_tokens = Some(maxTokens),
          n = Some(1),
          stop = Nil
        )
      ).map { response =>
        response.choices.head.message.content.strip()
      }
    }
  )

  override def modelName: String = s"OpenAI: ${modelId}"
}

object OpenAICompletionProvider {
  def apply(
    modelName: String,
    temperature: Double,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): CompletionProvider =
    new OpenAICompletionProvider(modelName, temperature, config)
}