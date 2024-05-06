package io.cequence.azureform

class AzureFormRecognizerClientException(
  message: String,
  cause: Throwable
) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)
}

class AzureFormRecognizerClientTimeoutException(
  message: String,
  cause: Throwable
) extends AzureFormRecognizerClientException(message, cause) {

  def this(message: String) = this(message, null)
}

class AzureFormRecognizerClientUnknownHostException(
  message: String,
  cause: Throwable
) extends AzureFormRecognizerClientException(message, cause) {

  def this(message: String) = this(message, null)
}

