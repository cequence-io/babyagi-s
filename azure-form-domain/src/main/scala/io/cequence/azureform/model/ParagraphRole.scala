package io.cequence.azureform.model

import io.cequence.wsclient.domain.EnumValue

sealed trait ParagraphRole extends EnumValue

object ParagraphRole {

  case object title extends ParagraphRole
  case object sectionHeading extends ParagraphRole
  case object footnote extends ParagraphRole
  case object pageHeader extends ParagraphRole
  case object pageFooter extends ParagraphRole
  case object pageNumber extends ParagraphRole
}
