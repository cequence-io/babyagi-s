package io.cequence.jinaapi.model

case class CrawlResponse(
  code: Int,
  status: Int,
  data: CrawlData
)

case class CrawlData(
  title: String,
  description: String,
  url: String,
  content: String,
  usage: Usage
)
