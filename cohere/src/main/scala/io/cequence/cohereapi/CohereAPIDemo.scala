package io.cequence.cohereapi

import io.cequence.cohereapi.model._
import io.cequence.cohereapi.service.CohereServiceFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CohereAPIDemo extends App {

  private val cohereService = CohereServiceFactory()

  // a JDK-engine HTTP client and the global EC run on daemon threads - wait for the run
  private val run = {
    for {
      embedResponse <- cohereService.embed(
        texts = Seq(
          "hello",
          "goodbye"
        ),
        settings = EmbedSettings(
          model = EmbedModelId.embed_english_v3_0,
          input_type = Some(InputType.classification)
        )
      )

      rerankResponse <- cohereService.rerank(
        query = "What is the capital of the United States?",
        documents = Seq(
          Map(
            "xyz" -> 2,
            "text" -> "Carson City is the capital city of the American state of Nevada."
          ),
          Map(
            "xyz" -> 9,
            "text" -> "The Commonwealth of the Northern Mariana Islands is a group of islands in the Pacific Ocean. Its capital is Saipan."
          ),
          Map(
            "xyz" -> 36,
            "text" -> "Washington, D.C. (also known as simply Washington or D.C., and officially as the District of Columbia) is the capital of the United States. It is a federal district."
          ),
          Map(
            "xyz" -> 22,
            "text" -> "Capitalization or capitalisation in English grammar is the use of a capital letter at the start of a word. English usage varies from capitalization in other languages."
          ),
          Map(
            "xyz" -> -5,
            "text" -> "Capital punishment (the death penalty) has existed in the United States since beforethe United States was a country. As of 2017, capital punishment is legal in 30 of the 50 states."
          )
        ),
        settings = RerankSettings(
          model = RerankModelId.rerank_v3_5,
          top_n = Some(3),
          return_documents = Some(true)
        )
      )
    } yield {
      println("Embed response:")
      println("texts: " + embedResponse.texts.mkString(", "))
      println("embeddings size: " + embedResponse.embeddings.map(_.size).mkString(", "))
      println

      println("Rerank response:")
      rerankResponse.results.foreach(println(_))
      cohereService.close()
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    println(ex.getMessage)
    cohereService.close()
    System.exit(1)
  }

  Await.ready(run, Duration.Inf)
}
