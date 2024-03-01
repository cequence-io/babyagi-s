package io.cequence.babyagis.next.domain

sealed trait VectorStoreSettings

// Pinecone
case class PineconeVectorStoreSettings(
  indexName: String,
  namespace: String
) extends VectorStoreSettings

// Local/dummy
object LocalVectorStoreSettings extends VectorStoreSettings