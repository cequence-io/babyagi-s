package io.cequence.cohereapi.model

import io.cequence.wsclient.domain.EnumValue

sealed trait TruncateType extends EnumValue

object TruncateType {
  case object NONE extends TruncateType
  case object START extends TruncateType
  case object END extends TruncateType
}
