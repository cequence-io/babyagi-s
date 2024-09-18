package io.cequence.jinaapi.service

import io.cequence.jinaapi.model.{CrawlerSettings, RerankerId, RerankerSettings, SegmenterSettings}

trait JinaConsts {
  object Defaults {
    val Crawl = CrawlerSettings()

    val Segmenter = SegmenterSettings()

    val Reranker = RerankerSettings(model = RerankerId.jina_colbert_v2, top_n = 50)
  }
}