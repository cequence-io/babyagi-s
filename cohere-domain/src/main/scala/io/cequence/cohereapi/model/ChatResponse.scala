package io.cequence.cohereapi.model

import java.util.UUID

case class ChatResponse(
  id: UUID,
  reply: String,
  meta: ResponseMeta
)