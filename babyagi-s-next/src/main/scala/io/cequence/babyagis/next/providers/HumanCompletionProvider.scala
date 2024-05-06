package io.cequence.babyagis.next.providers

import scala.concurrent.{ExecutionContext, Future}

private class HumanCompletionProvider(
  implicit ec: ExecutionContext
) extends CompletionProvider {

  override def apply(
    prompt: String,
    maxTokens: Int
  ) = Future {
    println(
      "\u001b[94m\u001b[1m" + "\n> COPY FOLLOWING TEXT TO CHATBOT\n" + "\u001b[0m\u001b[0m"
    )
    println(prompt)
    println(
      "\u001b[91m\u001b[1m" + "\n AFTER PASTING, PRESS: (ENTER / EMPTY LINE) TO FINISH\n" + "\u001b[0m\u001b[0m"
    )
    println("\u001b[96m\u001b[1m" + "\n> PASTE YOUR RESPONSE:\n" + "\u001b[0m\u001b[0m")

    val input_text = Stream.continually(scala.io.StdIn.readLine()).takeWhile(_.strip != "")
    input_text.mkString("\n").strip
  }

  override def modelName = "human"
}

object HumanCompletionProvider {
  def apply(
  )(
    implicit ec: ExecutionContext
  ): CompletionProvider =
    new HumanCompletionProvider()
}
