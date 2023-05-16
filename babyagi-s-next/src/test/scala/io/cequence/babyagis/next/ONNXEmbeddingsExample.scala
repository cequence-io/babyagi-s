package io.cequence.babyagis.next

import io.cequence.babyagis.next.providers.ONNXEmbeddingsProvider

import scala.concurrent.ExecutionContext

// TODO: turn this into a test
object ONNXEmbeddingsExample extends App {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val e5LargeTokenizerPath = "..."
  private val e5LargeModelPath = "..."

  private val embeddingsProvider = ONNXEmbeddingsProvider(
    e5LargeTokenizerPath,
    e5LargeModelPath,
    normalize = true,
    modelDisplayName = "E5-large"
  )

  val inputTexts = Seq(
    "query: how much protein should a female eat",
    "query: summit define",
    "passage: As a general guideline, the CDC's average requirement of protein for women ages 19 to 70 is 46 grams per day. But, as you can see from this chart, you'll need to increase that if you're expecting or training for a marathon. Check out the chart below to see how much protein you should be eating each day.",
    "passage: Definition of summit for English Language Learners. : 1  the highest point of a mountain : the top of a mountain. : 2  the highest level. : 3  a meeting or series of meetings between the leaders of two or more governments."
  )

  embeddingsProvider.apply(inputTexts).map { embeddings =>
    println(embeddings.size)
    embeddings.map(embeds =>
      println(embeds.take(3).mkString(", ") + "..." + embeds.takeRight(3).mkString(", "))
    )
  }

  Thread.sleep(10000)
}
