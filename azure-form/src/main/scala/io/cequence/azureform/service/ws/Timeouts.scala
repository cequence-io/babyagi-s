package io.cequence.azureform.service.ws

case class Timeouts(
  requestTimeout: Option[Int] = None,
  readTimeout: Option[Int] = None,
  connectTimeout: Option[Int] = None,
  pooledConnectionIdleTimeout: Option[Int] = None
)
