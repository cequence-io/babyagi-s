package io.cequence.babyagis.port

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.cequence.pineconescala.service.PineconeIndexServiceFactory

import scala.concurrent.{ExecutionContext, Future}

object PineconeIndexExample extends App {

  implicit val ec = ExecutionContext.global
  implicit val materializer = Materializer(ActorSystem())

  private val service = PineconeIndexServiceFactory()

  {
    for {
      indexes <- service.listIndexes

      _ = println(indexes.mkString(", "))

      _ <- service.describeIndex(indexes(0)).map(println(_))
    } yield {
      System.exit(0)
    }
  } recover {
    case e: Throwable =>
      println(e)
      System.exit(1)
  }
}
