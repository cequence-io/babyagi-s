package io.cequence.babyagis.next.providers

import scala.concurrent.Future

trait LLMProvider {

  def modelName: String

  def createCompletion(
    prompt: String,
    maxTokens: Int
  ): Future[String]

  def createEmbeddings(
    input: Seq[String]
  ): Future[Seq[Seq[Double]]]
}