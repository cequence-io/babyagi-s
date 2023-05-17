package io.cequence.babyagis.next

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{TextMessage, WebSocketRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import io.cequence.babyagis.next.providers.{DummyVectorStoreProvider, HumanCompletionProvider, ONNXEmbeddingsProvider, OpenAICompletionProvider, OpenAIEmbeddingsProvider, PineconeVectorStoreProvider}
import io.cequence.openaiscala.domain.ModelId
import akka.http.scaladsl.model.ws.{Message => WsMessage}
import io.cequence.pineconescala.service.PineconeIndexServiceFactory
import akka.http.scaladsl.server.Directives._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

object BabyAGIExample extends App {

  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = Materializer(actorSystem)

  // using baby-agi-s-config.conf, which expects:
  // - OPENAI_SCALA_CLIENT_API_KEY
  // - OPENAI_SCALA_CLIENT_ORG_ID (optional)
  // - PINECONE_SCALA_CLIENT_API_KEY
  // - PINECONE_SCALA_CLIENT_ENV
  private val config = ConfigFactory.load("baby-agi-s-config.conf")

  //////////////
  // SETTINGS //
  //////////////
  private val objective = "Save the planet Earth from the evil aliens"
  private val initialTask = "Develop a task list"
  private val pineconeIndexName = "baby-agi-test-table"
  private val pineconeNamespace = "xxx yyy zzz"
  //////////////

  // Completion provider
  private val completionProvider = OpenAICompletionProvider(
    modelName = ModelId.gpt_3_5_turbo,
    temperature = 0,
    config
  )

//  private val completionProvider = HumanCompletionProvider()

  // Embeddings provider
  private val embeddingsProvider = OpenAIEmbeddingsProvider(ModelId.text_embedding_ada_002, config)

//  private val embeddingsProvider = ONNXEmbeddingsProvider(config)


  // vector store: only Pinecone supported for now
  val vectorStoreFuture = for {
//    _ <- PineconeIndexServiceFactory(config).deleteIndex(pineconeIndexName)

    // check the embeddings' dimension
    embeddings <- embeddingsProvider(Seq("Hello world"))
    dimension = embeddings.head.size

    vectorStore <- PineconeVectorStoreProvider(
      pineconeIndexName,
      pineconeNamespace,
      dimension,
      config
    )
  } yield
    vectorStore

  private val vectorStore = Await.result(vectorStoreFuture, 1.minutes) // new DummyVectorStoreProvider()

  private val babyAGI = new BabyAGI(
    objective,
    initialTask,
    vectorStore,
    completionProvider,
    embeddingsProvider
  )

  val eventQueue = babyAGI.getEventQueue

  private val flowProcess = Flow[EventInfo].map { event =>
    event match {
      case Message(text) => println("MESSAGE RECEIVED: " + text)
    }
  }

  // make it running
  eventQueue.via(flowProcess).to(Sink.ignore).run()

  private val flowWsProcess: Flow[EventInfo, WsMessage, NotUsed] = Flow[EventInfo].map { event =>
    event match {
      case Message(text) => TextMessage.Strict(text)
    }
  }

  val websocketRoute =
    path("babyAGI") {
      handleWebSocketMessages(
        Flow.fromSinkAndSource(
          Sink.foreach(println),
          eventQueue.via(flowWsProcess))
      )
    }

  Http().newServerAt("127.0.0.1", 8888)
//    .adaptSettings(_.mapWebsocketSettings(_.withPeriodicKeepAliveData(() => ByteString(s"debug-${pingCounter.incrementAndGet()}"))))
    .bind(websocketRoute)

  babyAGI.exec
}