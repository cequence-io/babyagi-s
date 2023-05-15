package io.cequence.babyagis.next.providers

import akka.stream.Materializer
import com.typesafe.config.Config
import io.cequence.openaiscala.OpenAIScalaClientException
import io.cequence.openaiscala.domain.settings.{CreateChatCompletionSettings, CreateCompletionSettings, CreateEmbeddingsSettings}
import io.cequence.openaiscala.domain.{ChatRole, MessageSpec}
import io.cequence.openaiscala.service.OpenAIServiceFactory

import scala.concurrent.{ExecutionContext, Future}

private class OpenAILLMProvider(
  completionModel: String,
  embeddingModel: String,
  temperature: Double,
  config: Config)(
  implicit ec: ExecutionContext, materializer: Materializer
) extends LLMProvider {

  private val openAIService = OpenAIServiceFactory(config)

  private val maxRetryAttempts = 10
  private val sleepOnFailureSec = 10

  override def createCompletion(
    prompt: String,
    maxTokens: Int
  ): Future[String] = retryAux(
    if (!completionModel.toLowerCase.startsWith("gpt-")) {
      // Use completion API
      openAIService.createCompletion(
        prompt = prompt,
        settings = CreateCompletionSettings(
          model = completionModel,
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
          model = completionModel,
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

  override def createEmbeddings(
    input: Seq[String]
  ): Future[Seq[Seq[Double]]] = retryAux(
    openAIService.createEmbeddings(
      input = input,
      settings = CreateEmbeddingsSettings(embeddingModel)
    ).map(
      _.data.map(_.embedding)
    )
  )

  def retryAux[T](f: => Future[T]) =
    retryOnOpenAIException(
      failureMessage = "OpenAI API error occurred.",
      log = println(_),
      maxAttemptNum = maxRetryAttempts,
      sleepOnFailureMs = sleepOnFailureSec * 1000
    )(f)

  private def retryOnOpenAIException[T](
    failureMessage: String,
    log: String => Unit,
    maxAttemptNum: Int,
    sleepOnFailureMs: Int)(
    f: => Future[T])(
    implicit ec: ExecutionContext
  ): Future[T] = {
    def retryAux(attempt: Int): Future[T] =
      f.recoverWith {
        case e: OpenAIScalaClientException =>
          if (attempt < maxAttemptNum) {
            val errorMessage = e.getMessage.split("\n").find(_.contains("message")).map(
              _.trim.stripPrefix("\"message\": \"").stripSuffix("\",")
            ).getOrElse("")

            log(s"${failureMessage} ${errorMessage}. Attempt ${attempt}. Waiting ${sleepOnFailureMs / 1000} seconds")
            Thread.sleep(sleepOnFailureMs)
            retryAux(attempt + 1)
          } else
            throw e
      }

    retryAux(1)
  }

  override def modelName: String = s"OpenAI: ${completionModel}"
}

object OpenAILLMProvider {
  def apply(
    completionModel: String,
    embeddingModel: String,
    temperature: Double,
    config: Config)(
    implicit ec: ExecutionContext, materializer: Materializer
  ): LLMProvider =
    new OpenAILLMProvider(
      completionModel,
      embeddingModel,
      temperature,
      config
    )
}