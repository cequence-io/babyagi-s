package io.cequence.jinaapi

class JinaClientException(
  message: String,
  cause: Throwable
) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)
}

class JinaClientTimeoutException(
  message: String,
  cause: Throwable
) extends JinaClientException(message, cause) {

  def this(message: String) = this(message, null)
}

class JinaClientUnknownHostException(
  message: String,
  cause: Throwable
) extends JinaClientException(message, cause) {

  def this(message: String) = this(message, null)
}
