package io.cequence.cohereapi.model

case class Connector(
  // The identifier of the connector.
  id: String,
  
  // When specified, this user access token will be passed to the connector
  // in the Authorization header instead of the Cohere generated one.
  user_access_token: Option[String] = None,
  
  // When true, the request will continue if this connector returned an error.
  // Defaults to false.
  continue_on_failure: Boolean = false,
  
  // Provides the connector with different settings at request time.
  // The key/value pairs of this object are specific to each connector.
  // For example, the connector web-search supports the site option,
  // which limits search results to the specified domain.
  options: Map[String, Any] = Map.empty
)
