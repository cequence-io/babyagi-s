package io.cequence.matrix

import io.cequence.cohereapi.service.CohereServiceFactory
import io.cequence.mistral.service.MistralServiceFactory
import io.cequence.wsclient.service.spi.{TransportSettings, WSClientEngineRegistry}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

// The README snippet: one explicitly chosen engine (-Dws-client.engine) shared by two services,
// still usable after the services are closed. Expects MISTRAL_API_KEY and COHERE_API_KEY.
object SharedEngineSmoke extends App {

  private val engine = WSClientEngineRegistry(TransportSettings(), sys.props.get("ws-client.engine"))
  println(s"Engine: ${engine.getClass.getName}")

  private val mistral = MistralServiceFactory.withEngine(engine, apiKey = sys.env("MISTRAL_API_KEY"))
  private val cohere = CohereServiceFactory.withEngine(engine, apiKey = sys.env("COHERE_API_KEY"))

  // the JDK engine and the global EC run on daemon threads - wait for the run
  private val run = {
    for {
      files <- mistral.listFiles(Some(0), Some(1))
      embed <- cohere.embed(Seq("ws-client engine smoke"))
      _ = { mistral.close(); cohere.close() }
      // a service never closes an engine it was given
      afterClose <- MistralServiceFactory.withEngine(engine, apiKey = sys.env("MISTRAL_API_KEY")).listFiles(Some(0), Some(1))
    } yield {
      println(s"mistral files: ${files.total}, cohere embeddings: ${embed.embeddings.size}, after close: ${afterClose.total}")
      println("OK")
      engine.close()
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    ex.printStackTrace()
    engine.close()
    System.exit(1)
  }

  Await.ready(run, Duration.Inf)
}
