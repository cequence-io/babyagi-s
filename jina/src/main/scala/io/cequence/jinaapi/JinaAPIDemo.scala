package io.cequence.jinaapi

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import io.cequence.jinaapi.model._
import io.cequence.jinaapi.service.{JinaChunkingRegex, JinaServiceFactory}

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

object JinaAPIDemo extends App {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = ActorMaterializer()

  private val service = JinaServiceFactory()

  private val testHtmlFile = "xxx"

  private val testPdfFile = "xxxx"

  private val hamletFile = new File(getClass.getResource("/hamlet.txt").getFile)
  private val hamletContent =
    scala.io.Source.fromFile(hamletFile).mkString.take(20000)

  private val rerankDocuments = Seq(
    "Organic skincare for sensitive skin with aloe vera and chamomile: Imagine the soothing embrace of nature with our organic skincare range, crafted specifically for sensitive skin. Infused with the calming properties of aloe vera and chamomile, each product provides gentle nourishment and protection. Say goodbye to irritation and hello to a glowing, healthy complexion.",
    "New makeup trends focus on bold colors and innovative techniques: Step into the world of cutting-edge beauty with this seasons makeup trends. Bold, vibrant colors and groundbreaking techniques are redefining the art of makeup. From neon eyeliners to holographic highlighters, unleash your creativity and make a statement with every look.",
    "Bio-Hautpflege für empfindliche Haut mit Aloe Vera und Kamille: Erleben Sie die wohltuende Wirkung unserer Bio-Hautpflege, speziell für empfindliche Haut entwickelt. Mit den beruhigenden Eigenschaften von Aloe Vera und Kamille pflegen und schützen unsere Produkte Ihre Haut auf natürliche Weise. Verabschieden Sie sich von Hautirritationen und genießen Sie einen strahlenden Teint.",
    "Neue Make-up-Trends setzen auf kräftige Farben und innovative Techniken: Tauchen Sie ein in die Welt der modernen Schönheit mit den neuesten Make-up-Trends. Kräftige, lebendige Farben und innovative Techniken setzen neue Maßstäbe. Von auffälligen Eyelinern bis hin zu holografischen Highlightern – lassen Sie Ihrer Kreativität freien Lauf und setzen Sie jedes Mal ein Statement.",
    "Cuidado de la piel orgánico para piel sensible con aloe vera y manzanilla: Descubre el poder de la naturaleza con nuestra línea de cuidado de la piel orgánico, diseñada especialmente para pieles sensibles. Enriquecidos con aloe vera y manzanilla, estos productos ofrecen una hidratación y protección suave. Despídete de las irritaciones y saluda a una piel radiante y saludable.",
    "Las nuevas tendencias de maquillaje se centran en colores vivos y técnicas innovadoras: Entra en el fascinante mundo del maquillaje con las tendencias más actuales. Colores vivos y técnicas innovadoras están revolucionando el arte del maquillaje. Desde delineadores neón hasta iluminadores holográficos, desata tu creatividad y destaca en cada look.",
    "针对敏感肌专门设计的天然有机护肤产品：体验由芦荟和洋甘菊提取物带来的自然呵护。我们的护肤产品特别为敏感肌设计，温和滋润，保护您的肌肤不受刺激。让您的肌肤告别不适，迎来健康光彩。",
    "新的化妆趋势注重鲜艳的颜色和创新的技巧：进入化妆艺术的新纪元，本季的化妆趋势以大胆的颜色和创新的技巧为主。无论是霓虹眼线还是全息高光，每一款妆容都能让您脱颖而出，展现独特魅力。",
    "敏感肌のために特別に設計された天然有機スキンケア製品: アロエベラとカモミールのやさしい力で、自然の抱擁を感じてください。敏感肌用に特別に設計された私たちのスキンケア製品は、肌に優しく栄養を与え、保護します。肌トラブルにさようなら、輝く健康な肌にこんにちは。",
    "新しいメイクのトレンドは鮮やかな色と革新的な技術に焦点を当てています: 今シーズンのメイクアップトレンドは、大胆な色彩と革新的な技術に注目しています。ネオンアイライナーからホログラフィックハイライターまで、クリエイティビティを解き放ち、毎回ユニークなルックを演出しましょう。"
  )

  {
    for {
      crawlResponse1 <- service.crawl(
        "https://www.cnn.com/",
        CrawlSourceType.url,
      )

      _ = println(crawlResponse1)

      crawlStringResponse1 <- service.crawlString(
        "https://www.cnn.com/",
        CrawlSourceType.url,
      )

      _ = println(crawlStringResponse1)

      crawlResponse2 <- service.crawl(
        Source.fromFile(testHtmlFile).mkString,
        CrawlSourceType.html,
      )

      _ = println(crawlResponse2)

      crawlResponse3 <- service.crawl(
        Source.fromFile(testPdfFile).mkString,
        CrawlSourceType.pdf,
      )

      _ = println(crawlResponse3)

      segmentResponse <- service.segment(
        hamletContent,
        settings = SegmenterSettings(
          returnTokens = Some(true),
          returnChunks = Some(true),
          maxChunkLength = Some(1000)
        )
      )

      matches = JinaChunkingRegex.regex.findAllMatchIn(hamletContent).toList

      rerankResponse <- service.rerank(
        "Organic skincare products for sensitive skin",
        rerankDocuments
      )
    } yield {
      println("Chunks:")
      println(segmentResponse.chunks.take(10).mkString("----------------\n"))

      println()

      val chunksStarts = Seq(0) ++ matches.map(_.start)

      val chunks2 = chunksStarts.zipWithIndex.map { case (start, index) =>
        val end = if (index == chunksStarts.length - 1) hamletContent.length else chunksStarts(index + 1)
        hamletContent.substring(start, end)
      }

      println("Chunks2:")
      println(chunks2.take(10).mkString("----------------\n"))

      println()

      println("Rerank response:")
      println(rerankResponse.results.mkString("\n----------------\n"))

      service.close()
      System.exit(0)
    }
  }.recover { case ex: Throwable =>
    println(ex.getMessage)
    service.close()
    System.exit(1)
  }
}