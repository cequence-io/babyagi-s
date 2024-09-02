package io.cequence.cohereapi

class CohereClientException(
  message: String,
  cause: Throwable
) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)
}

class CohereClientTimeoutException(
  message: String,
  cause: Throwable
) extends CohereClientException(message, cause) {

  def this(message: String) = this(message, null)
}

class CohereClientUnknownHostException(
  message: String,
  cause: Throwable
) extends CohereClientException(message, cause) {

  def this(message: String) = this(message, null)
}
