package io.cequence.babyagis.next.providers

import scala.concurrent.{ExecutionContext, Future}

trait PineconeHelper {

  protected val maxRetryAttempts = 3
  protected val sleepOnFailureSec = 5

  protected def retryAux[T](
     f: => Future[T])(
     implicit ec: ExecutionContext
  ) =
    retry(
      failureMessage = "Pinecone API call error occurred.",
      log = println(_),
      maxAttemptNum = maxRetryAttempts,
      sleepOnFailureMs = sleepOnFailureSec * 1000
    )(f)

  // TODO: move somewhere else
  private def retry[T](
    failureMessage: String,
    log: String => Unit,
    maxAttemptNum: Int,
    sleepOnFailureMs: Int)(
    f: => Future[T])(
    implicit ec: ExecutionContext
  ): Future[T] = {
    def retryAux(attempt: Int): Future[T] =
      f.recoverWith {
        case e: Exception =>
          if (attempt < maxAttemptNum) {
            log(s"${failureMessage}. ${e.getMessage}. Attempt ${attempt}. Retrying...")

            Thread.sleep(sleepOnFailureMs)

            retryAux(attempt + 1)
          } else
            throw e
      }

    retryAux(1)
  }
}