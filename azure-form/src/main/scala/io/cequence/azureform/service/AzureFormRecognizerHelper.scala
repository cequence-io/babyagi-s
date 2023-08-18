package io.cequence.azureform.service

import io.cequence.azureform.model._
import org.kynosarges.tektosyne.geometry.{GeoUtils, PointD, PolygonLocation}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait AzureFormRecognizerHelper {

  protected val linesPerPageForNewLineThreshold = 50
  private val logger = LoggerFactory.getLogger(this.getClass)

  def pollUntilDone[T <: HasStatus](
    call: => Future[T]
  )(
    implicit ec: ExecutionContext
  ): Future[T] =
    call.flatMap(result =>
      result.status match {
        case "running" =>
          Thread.sleep(500) // TODO: use scheduler
          pollUntilDone(call)
        case _ => Future(result)
      }
    )

  protected def extractInvoiceEntities(
    invoiceAnalyzeResult: InvoiceAnalyzeResult
  ) = {
    val invoiceFields = invoiceAnalyzeResult.documents.head.fields

    def getValue(f: InvoiceFields => Option[IsContentEntry]) =
      f(invoiceFields).map(_.content)

    Seq(
      ("ServiceAddress", getValue(_.ServiceAddress)),
      ("ServiceAddressRecipient", getValue(_.ServiceAddressRecipient)),
      ("PreviousUnpaidBalance", getValue(_.PreviousUnpaidBalance)),
      ("RemittanceAddressRecipient", getValue(_.RemittanceAddressRecipient)),
      ("InvoiceId", getValue(_.InvoiceId)),
      ("SubTotal", getValue(_.SubTotal)),
      ("BillingAddress", getValue(_.BillingAddress)),
      ("TotalTax", getValue(_.TotalTax)),
      ("ServiceStartDate", getValue(_.ServiceStartDate)),
      ("CustomerName", getValue(_.CustomerName)),
      ("InvoiceDate", getValue(_.InvoiceDate)),
      ("DueDate", getValue(_.DueDate)),
      ("CustomerAddressRecipient", getValue(_.CustomerAddressRecipient)),
      ("RemittanceAddress", getValue(_.RemittanceAddress)),
      ("AmountDue", getValue(_.AmountDue)),
      ("VendorName", getValue(_.VendorName)),
      ("ServiceEndDate", getValue(_.ServiceEndDate)),
      ("CustomerId", getValue(_.CustomerId)),
      ("VendorAddressRecipient", getValue(_.VendorAddressRecipient)),
      ("ShippingAddressRecipient", getValue(_.ShippingAddressRecipient)),
      ("InvoiceTotal", getValue(_.InvoiceTotal)),
      ("ShippingAddress", getValue(_.ShippingAddress)),
      ("BillingAddressRecipient", getValue(_.BillingAddressRecipient)),
      ("PurchaseOrder", getValue(_.PurchaseOrder)),
      ("VendorAddress", getValue(_.VendorAddress)),
      ("CustomerAddress", getValue(_.CustomerAddress))
    )
  }
  protected def replaceWithEnhancedContent(
    layoutResult: LayoutAnalyzeResult,
    tableInfos: Seq[TableInfo]
  ): Seq[Seq[String]] =
    // find table lines and replace them with enriched table content
    layoutResult.pages.map { page =>
      val height = page.height
      val pageNumber = page.pageNumber
      val pageTableInfos = tableInfos.filter { case TableInfo(tablePageNumber, _, _, _) =>
        tablePageNumber == pageNumber
      }

      page.lines.zipWithIndex.flatMap { case (line, index) =>
        val tableInfo = pageTableInfos.find { tableInfo =>
          index >= tableInfo.firstLineIndex && index <= tableInfo.lastLineIndex
        }

        tableInfo match {
          case Some(tableInfo) =>
            if (tableInfo.firstLineIndex == index)
              tableInfo.newLines
            else
              Nil

          case None =>
            if (index > 0) {
              val prevLine = page.lines(index - 1)

              val prevYTop = Math.min(prevLine.polygon(1), prevLine.polygon(3))
              val prevYBottom = Math.max(prevLine.polygon(5), prevLine.polygon(7))

              val yTop = Math.min(line.polygon(1), line.polygon(3))
              val yBottom = Math.max(line.polygon(5), line.polygon(7))

              val prevLineHeight = Math.abs(prevYTop - prevYBottom)
              val lineHeight = Math.abs(yTop - yBottom)

              val avgLineHeight = (prevLineHeight + lineHeight) / 2

              // add a new line if a gap between lines is too big
              if (Math.abs(yTop - prevYBottom) > avgLineHeight)
                Seq("", line.content)
              else
                Seq(line.content)
            } else
              Seq(line.content)
        }
      }
    }

  protected def extractEnhancedContent(
    readResult: ReadAnalyzeResult
  ): Seq[Seq[String]] =
    // find table lines and replace them with enriched table content
    readResult.pages.map { page =>
      val height = page.height
      val pageNumber = page.pageNumber

      val paragraphsWithPageNums = readResult.paragraphs.map { par =>
        val paragraphPageNumber = par.boundingRegions.head.pageNumber
        (par, paragraphPageNumber)
      }

      paragraphsWithPageNums.filter(_._2 == pageNumber).zipWithIndex.flatMap {
        case ((par, paragraphPageNumber), index) =>
          val paragraphPolygon = par.boundingRegions.head.polygon

          if (index > 0) {
            val prevParagraph = readResult.paragraphs(index - 1)
            val prevParagraphPageNumber = prevParagraph.boundingRegions.head.pageNumber

            if (paragraphPageNumber == prevParagraphPageNumber) {
              val prevParagraphPolygon = prevParagraph.boundingRegions.head.polygon

              val prevYBottom = Math.max(prevParagraphPolygon(5), prevParagraphPolygon(7))
              val yTop = Math.min(paragraphPolygon(1), paragraphPolygon(3))

              // add a new line if a gap between lines is too big
              if (Math.abs(yTop - prevYBottom) > height / linesPerPageForNewLineThreshold)
                Seq("", par.content)
              else
                Seq(par.content)
            } else
              Seq(par.content)
          } else
            Seq(par.content)
      }
    }

  protected def extractEnhancedContent(
    layoutAnalyzeResult: LayoutAnalyzeResult
  ): Seq[Seq[String]] = {
    val tableInfos =
      layoutAnalyzeResult.tables.zipWithIndex.map { case (table, tableIndex) =>
        val pageNumber = table.boundingRegions.head.pageNumber
        val tableLines = findTableLinesAndValidate(layoutAnalyzeResult, table, tableIndex)
        val originalTableLineContents = tableLines.map(_._1.content)
        val newLines = tableToLines(table, originalTableLineContents, rowDelimiter = Some("--"))
        TableInfo(pageNumber, tableLines.head._2, tableLines.last._2, newLines)
      }

    replaceWithEnhancedContent(layoutAnalyzeResult, tableInfos)
  }

  protected def findTableLinesAndValidate(
    layoutAnalyzeResult: LayoutAnalyzeResult,
    table: Table,
    tableIndex: Int
  ) = {
    val tableLines = findTableLines(layoutAnalyzeResult, table)
    if (tableLines.isEmpty)
      throw new IllegalArgumentException(s"Table ${tableIndex} has no lines")

    val tablePageNumber = table.boundingRegions.head.pageNumber

    val pageLines = layoutAnalyzeResult.pages
      .find(_.pageNumber == tablePageNumber)
      .getOrElse(
        throw new IllegalArgumentException(s"Page ${tablePageNumber} not found")
      )
      .lines

    // validate if there are no gaps between table lines
    val tableLinesIndexes = tableLines.map(_._2)
    val tableLinesIndexesRange = tableLinesIndexes.head to tableLinesIndexes.last
    val tableLinesIndexesRangeDiff = tableLinesIndexesRange.diff(tableLinesIndexes)
    if (tableLinesIndexesRangeDiff.nonEmpty) {
      val diffContents = pageLines
        .zipWithIndex
        .filter { case (_, index) =>
          tableLinesIndexesRangeDiff.contains(index)
        }
        .map(_._1.content)

      logger.warn(
        s"Table ${tableIndex} at the page ${tablePageNumber} has gaps at lines: ${tableLinesIndexesRangeDiff
            .mkString(", ")} - contents: ${diffContents.mkString(", ")}"
      )
    }

    //    // validate if there is a line for each cell in the table
    //    if (tableLines.size != table.cells.size) {
    //      throw new IllegalArgumentException(
    //        s"Table ${tableIndex} at the page ${tablePageNumber} has ${tableLines.size} lines (extracted from the page) but ${table.cells.size} cells"
    //      )
    //    }

    // compare line content with cell content
    val originalTableLineContents = tableLines.map(_._1.content)
    val tableLinesString = originalTableLineContents.mkString(" ").replaceAll("\\s+", " ").trim
    val tableCellsString = tableToLinesContent(table, originalTableLineContents, None).mkString(" ").replaceAll("\\s+", " ").trim
    if (tableLinesString != tableCellsString) {
      logger.warn(
        s"Table ${tableIndex} at the page ${tablePageNumber} has lines:\n'${tableLinesString}'\nbut cells:\n'${tableCellsString}'"
      )
    }

    //    tableLines.zip(table.cells).foreach { case ((line, lineIndex), cell) =>
    //      if (line.content != cell.content) {
    //        throw new IllegalArgumentException(
    //          s"Table ${tableIndex} at the page ${tablePageNumber} has line ${lineIndex} with content ${line.content} but cell ${cell.rowIndex}x${cell.columnIndex} with content ${cell.content}"
    //        )
    //      }
    //    }

    tableLines
  }

  // find table lines
  protected def findTableLines(
    layoutAnalyzeResult: LayoutAnalyzeResult,
    table: Table
  ) = {
    val tablePageNumber = table.boundingRegions.head.pageNumber
    val tablePolygon = table.boundingRegions.head.polygon
    val tablePolygonCoordinates =
      tablePolygon.grouped(2).map { case Seq(x, y) => (x, y) }.toSeq

    val tablePage = layoutAnalyzeResult.pages
      .find(_.pageNumber == tablePageNumber)
      .getOrElse(
        throw new IllegalArgumentException(s"Page ${tablePageNumber} not found")
      )

    tablePage.lines.zipWithIndex.filter { case (line, index) =>
      val linePolygon = line.polygon.grouped(2).map { case Seq(x, y) => (x, y) }.toSeq
      isPolygonInside(tablePolygonCoordinates, linePolygon)
    }
  }

  private def tableToLines(
    table: Table,
    originalTableLines: Seq[String],
    rowDelimiter: Option[String]
  ) = {
    val cellLines = tableToLinesContent(table, originalTableLines, rowDelimiter)

    Seq("", s"Table ${table.rowCount} (rows) x ${table.columnCount} (columns):", "--") ++ cellLines
  }

  private def tableToLinesContent(
    table: Table,
    originalTableCells: Seq[String],
    afterRowLine: Option[String]
  ) = {
    val rowColumnIndexCellMap = table.cells.map { cell =>
      ((cell.rowIndex, cell.columnIndex), cell)
    }.toMap

    // s"${rowIndex} x ${columnIndex}, p Height: ${polygonHeight(polygon)}: " + cell.content

    var remainingOriginalTableCellContent = originalTableCells.mkString(" ").replaceAll("\\s+", " ").trim

    (0 until table.rowCount).flatMap { rowIndex =>
      (0 until table.columnCount).map { columnIndex =>
        rowColumnIndexCellMap
          .get((rowIndex, columnIndex))
          .map { cell =>
            // fix weird values from line above or bellow that occur once a while
            val content = if (cell.content.contains("\n")) {
              val options = cell.content.split("\n").map(_.trim)
              options.find(remainingOriginalTableCellContent.startsWith).getOrElse(options.head)
            } else {
              cell.content.trim
            }

            remainingOriginalTableCellContent = remainingOriginalTableCellContent.stripPrefix(content).trim

            content
          }.getOrElse(
            ""
          )
      } ++ (afterRowLine.map(Seq(_)).getOrElse(Nil))
    }
  }

  protected def isPolygonInside(
    outerPolygon: Seq[(Double, Double)],
    polygon: Seq[(Double, Double)]
  ): Boolean =
    polygon.forall(isInside(outerPolygon, _))

  private def isInside(
    polygon: Seq[(Double, Double)],
    point: (Double, Double)
  ): Boolean = {
    val polygonPoints = polygon.map { case (x, y) => new PointD(x, y) }
    val polygonLocation =
      GeoUtils.pointInPolygon(new PointD(point._1, point._2), polygonPoints.toArray)

    polygonLocation != PolygonLocation.OUTSIDE
  }

  private def polygonHeight(
    polygon: Seq[Double]
  ): Double = {
    val ys = Seq(polygon(1), polygon(3), polygon(5), polygon(7))
    ys.max - ys.min
  }
}

case class TableInfo(
  pageNumber: Int,
  firstLineIndex: Int,
  lastLineIndex: Int,
  newLines: Seq[String]
)