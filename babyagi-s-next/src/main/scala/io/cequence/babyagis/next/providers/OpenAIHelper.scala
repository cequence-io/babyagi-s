package io.cequence.babyagis.next.providers

import io.cequence.openaiscala.OpenAIScalaClientException
import scala.concurrent.{ExecutionContext, Future}
trait OpenAIHelper {

  protected val maxRetryAttempts = 10
  protected val sleepOnFailureSec = 10

  protected def retryAux[T](
    f: => Future[T])(
    implicit ec: ExecutionContext
  ) =
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
}
