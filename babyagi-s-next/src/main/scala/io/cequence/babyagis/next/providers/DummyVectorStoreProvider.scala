package io.cequence.babyagis.next.providers

import scala.concurrent.{ExecutionContext, Future}

class DummyVectorStoreProvider(implicit ec: ExecutionContext) extends VectorStoreProvider {

  override def add(
    id: String,
    vector: Seq[Double],
    metadata: Map[String, String]
  ) = Future(())

  override def querySorted(
    vector: Seq[Double],
    topResultsNum: Int,
    metadataFieldName: String
  ): Future[Seq[String]] = Future(Nil)
}