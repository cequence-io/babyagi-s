package io.cequence.babyagis.next

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import io.cequence.babyagis.next.providers.{OpenAILLMProvider, PineconeVectorStoreProvider}
import io.cequence.openaiscala.domain.ModelId

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

object BabyAGIExample extends App {

  private implicit val ec: ExecutionContext = ExecutionContext.global
  private val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = Materializer(actorSystem)

  // using baby-agi-s-config.conf which expects:
  // - OPENAI_SCALA_CLIENT_API_KEY, OPENAI_SCALA_CLIENT_ORG_ID (optional), PINECONE_SCALA_CLIENT_API_KEY, and PINECONE_SCALA_CLIENT_ENV
  private val config = ConfigFactory.load("baby-agi-s-config.conf")

  private val llmService = OpenAILLMProvider(
    completionModel = ModelId.gpt_3_5_turbo,
    embeddingModel = ModelId.text_embedding_ada_002,
    temperature = 0,
    config
  )

  private val vectorStoreFuture = PineconeVectorStoreProvider(
    indexName = "baby-agi-test-table",
    namespace = "xxx yyy zzz",
    config
  )

  private val vectorStore = Await.result(vectorStoreFuture, 1.minutes)

  private val babyAGI = new BabyAGI(
    objective = "Save the planet Earth from the evil aliens",
    initialTask = "Develop a task list",
    vectorStore,
    llmService
  )

  babyAGI.exec
}
