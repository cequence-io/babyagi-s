package io.cequence.babyagis.next.providers

import scala.concurrent.{ExecutionContext, Future}

private class HumanCompletionProvider(implicit ec: ExecutionContext) extends CompletionProvider {

  override def apply(
    prompt: String,
    maxTokens: Int
  ) = Future {
    println("\033[94m\033[1m" + "\n> COPY FOLLOWING TEXT TO CHATBOT\n" + "\033[0m\033[0m")
    println(prompt)
    println("\033[91m\033[1m" + "\n AFTER PASTING, PRESS: (ENTER / EMPTY LINE) TO FINISH\n" + "\033[0m\033[0m")
    println("\033[96m\033[1m" + "\n> PASTE YOUR RESPONSE:\n" + "\033[0m\033[0m")

    val input_text = Stream.continually(scala.io.StdIn.readLine()).takeWhile(_.strip != "")
    input_text.mkString("\n").strip
  }

  override def modelName = "human"
}

object HumanCompletionProvider {
  def apply()(implicit ec: ExecutionContext): CompletionProvider =
    new HumanCompletionProvider()
}