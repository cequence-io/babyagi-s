package io.cequence.jinaapi.service

import io.cequence.jinaapi.model._
import io.cequence.wsclient.service.CloseableService

import scala.concurrent.Future

trait JinaService extends JinaConsts with CloseableService {

  def crawl(
    input: String,
    sourceType: CrawlSourceType,
    settings: CrawlerSettings = Defaults.Crawl
  ): Future[CrawlResponse]

  def crawlString(
    input: String,
    sourceType: CrawlSourceType,
    settings: CrawlerSettings = Defaults.Crawl
  ): Future[String]

  /**
   * @param content
   *   Content length must be greater than 0 and less than 64k
   * @param settings
   * @return
   */
  def segment(
    content: String,
    settings: SegmenterSettings = Defaults.Segmenter
  ): Future[SegmenterResponse]

  def rerank(
    query: String,
    documents: Seq[String],
    settings: RerankerSettings = Defaults.Reranker
  ): Future[RerankerResponse]
}
