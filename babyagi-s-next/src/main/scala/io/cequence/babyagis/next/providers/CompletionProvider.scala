package io.cequence.babyagis.next.providers

import scala.concurrent.Future

trait CompletionProvider {

  def modelName: String

  def apply(
    prompt: String,
    maxTokens: Int
  ): Future[String]
}