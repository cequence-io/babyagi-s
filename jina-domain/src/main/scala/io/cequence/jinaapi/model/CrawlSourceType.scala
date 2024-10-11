package io.cequence.jinaapi.model

import io.cequence.wsclient.domain.EnumValue

sealed trait CrawlSourceType extends EnumValue

object CrawlSourceType {
  case object url extends CrawlSourceType
  case object html extends CrawlSourceType
  case object pdf extends CrawlSourceType
}
