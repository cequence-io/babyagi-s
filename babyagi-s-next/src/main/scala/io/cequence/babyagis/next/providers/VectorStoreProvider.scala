package io.cequence.babyagis.next.providers

import scala.concurrent.Future

trait VectorStoreProvider {

  def add(
    id: String,
    values: Seq[Double],
    metadata: Map[String, String]
  ): Future[Unit]

  def querySorted(
    vector: Seq[Double],
    topResultsNum: Int,
    metadataFieldName: String
  ): Future[Seq[String]]
}
