package io.cequence.azureform.model

trait PageElement {
  def contentString: String
}

case class TextElement(
  content: String
) extends PageElement {
  override def contentString = content
}

case class TableElement(
  tableNumber: Int,
  rowCount: Int,
  columnCount: Int,
  content: Seq[String]
) extends PageElement {
  override def contentString = content.mkString("\n")
}

case class PageContent(
  pageNumber: Int,
  width: Double,
  height: Double,
  elements: Seq[PageElementExt],
  words: Seq[PageTextExt] = Nil
)

trait PageElementExtTrait {
  val element: PageElement
  val box: BoundingBox
}

case class PageElementExt(
  element: PageElement,
  box: BoundingBox
) extends PageElementExtTrait

case class PageTextExt(
  element: TextElement,
  box: BoundingBox
) extends PageElementExtTrait
