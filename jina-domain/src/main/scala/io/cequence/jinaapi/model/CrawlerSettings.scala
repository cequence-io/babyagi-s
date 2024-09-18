package io.cequence.jinaapi.model

import io.cequence.wsclient.domain.EnumValue

case class CrawlerSettings(
  /**
   * Content Format You can control the level of detail in the response to prevent
   * over-filtering. The default pipeline is optimized for most websites and LLM input. enum:
   * markdown, html, text, screenshot, pageshot
   */
  contentFormat: Option[ContentFormat] = None,

  /**
   * Timeout Maximum time to wait for the webpage to load. Note that this is NOT the total time
   * for the whole end-to-end request.
   */
  timeout: Option[Int] = None,

  /**
   * Target Selector Provide a CSS selector to focus on a more specific part of the page.
   * Useful when your desired content doesn't show under the default settings.
   */
  targetSelector: Option[String] = None,

  /**
   * Wait For Selector Wait for a specific element to appear before returning. Useful when your
   * desired content doesn't show under the default settings.
   */
  waitForSelector: Option[String] = None,

  /**
   * Forward Cookie Our API server can forward your custom cookie settings when accessing the
   * URL, which is useful for pages requiring extra authentication. Note that requests with
   * cookies will not be cached.
   */
  forwardCookie: Option[String] = None,

  /**
   * Use a Proxy Server Our API server can utilize your proxy to access URLs, which is helpful
   * for pages accessible only through specific proxies.
   */
  proxyURL: Option[String] = None,

  /**
   * No Cache Our API server caches both Read and Search mode contents for a certain amount of
   * time. To bypass this cache, set this header to true.
   */
  noCache: Option[Boolean] = None,

  /**
   * Stream Mode Stream mode is beneficial for large target pages, allowing more time for the
   * page to fully render. If standard mode results in incomplete content, consider using
   * Stream mode.
   */
  streamMode: Option[Boolean] = None,

  /**
   * Browser Locale Control the browser locale to render the page. Lots of websites serve
   * different content based on the locale.
   */
  browserLocale: Option[String] = None
)

sealed trait ContentFormat extends EnumValue

object ContentFormat {
  case object markdown extends ContentFormat
  case object html extends ContentFormat
  case object text extends ContentFormat
  case object screenshot extends ContentFormat
  case object pageshot extends ContentFormat
}
