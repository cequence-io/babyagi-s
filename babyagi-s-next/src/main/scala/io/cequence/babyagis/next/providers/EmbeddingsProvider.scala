package io.cequence.babyagis.next.providers

import scala.concurrent.Future

trait EmbeddingsProvider {

  def modelName: String

  def apply(
    input: Seq[String]
  ): Future[Seq[Seq[Double]]]
}