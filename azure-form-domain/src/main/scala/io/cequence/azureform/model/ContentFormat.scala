package io.cequence.azureform.model

import io.cequence.wsclient.domain.EnumValue

sealed trait ContentFormat extends EnumValue

object ContentFormat {

  case object markdown extends ContentFormat

  case object text extends ContentFormat
}
