package io.cequence.mistral

class MistralClientException(
  message: String,
  cause: Throwable
) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)
}

class MistralClientTimeoutException(
  message: String,
  cause: Throwable
) extends MistralClientException(message, cause) {

  def this(message: String) = this(message, null)
}

class MistralClientUnknownHostException(
  message: String,
  cause: Throwable
) extends MistralClientException(message, cause) {

  def this(message: String) = this(message, null)
}
