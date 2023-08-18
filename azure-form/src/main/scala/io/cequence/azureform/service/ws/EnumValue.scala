package io.cequence.azureform.service.ws

abstract class EnumValue(value: String = "") {

  override def toString: String =
    if (value.nonEmpty) value else getClass.getSimpleName.stripSuffix("$")
}
