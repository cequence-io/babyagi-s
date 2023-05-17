package io.cequence.babyagis.next

sealed trait EventInfo
case class Message(text: String) extends EventInfo
