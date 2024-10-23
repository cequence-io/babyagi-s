package io.cequence.azureform.service

import io.cequence.azureform.model._
import org.slf4j.LoggerFactory
import io.cequence.wsclient.service.PollingHelper

import scala.concurrent.{ExecutionContext, Future}

trait AzureFormRecognizerHelper extends PolygonHelper with PollingHelper {

  object ReadModelDefaults {
    val linesPerPageForNewLineThreshold = 60
    val paragraphFixSortMinCoorDiffPercent = 0.5
    val paragraphFixOuterBoundingBoxExtraPaddingPercent = 5
    val minYOverlapPercent = 4
  }

  private val logParagraphsReplacement = true

  private val logger = LoggerFactory.getLogger(this.getClass)

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

  private def sortTopBottomLeftRight[T](
    bboxExts: Seq[(BoundingBox, T)],
    minCoorDiffPercent: Double = 0.1
  ): Seq[(BoundingBox, T)] = {
    def isSmallerAux(
      val1: Double,
      val2: Double
    ) = {
      val diffPercent = 100 * ((val2 - val1) / val1)
      diffPercent >= minCoorDiffPercent
    }

    def areEqualAux(
      val1: Double,
      val2: Double
    ) = {
      val diffPercent = 100 * ((val2 - val1) / val1)
      Math.abs(diffPercent) < minCoorDiffPercent
    }

    sortAux(isSmallerAux, isSmallerAux, areEqualAux, areEqualAux)(bboxExts)
  }

  protected def sortTopBottomLeftRightForPage[T](
    bboxExts: Seq[(BoundingBox, T)],
    width: Double,
    height: Double,
    minCoorDiffPercent: Double = 0.1
  ): Seq[(BoundingBox, T)] = {
    val minXCoorDiff = width * minCoorDiffPercent / 100
    val minYCoorDiff = height * minCoorDiffPercent / 100

    def isSmallerAux(
      minCoorDiff: Double
    )(
      val1: Double,
      val2: Double
    ) = {
      val diff = val2 - val1
      diff >= minCoorDiff
    }

    def areEqualAux(
      minCoorDiff: Double
    )(
      val1: Double,
      val2: Double
    ) = {
      val diff = val2 - val1
      Math.abs(diff) < minCoorDiff
    }

    sortAux(
      isSmallerAux(minXCoorDiff),
      isSmallerAux(minYCoorDiff),
      areEqualAux(minXCoorDiff),
      areEqualAux(minYCoorDiff)
    )(bboxExts)
  }

  protected def sortTopBottomLeftRightForPageInGroups[T](
    bboxExts: Seq[(BoundingBox, T)],
    width: Double,
    height: Double,
    minCoorDiffPercent: Double = 0.1
  ): Seq[(BoundingBox, T)] = {
    val minXCoorDiff = width * minCoorDiffPercent / 100
    val minYCoorDiff = height * minCoorDiffPercent / 100

    def isSmallerAux(
      minCoorDiff: Double
    )(
      val1: Double,
      val2: Double
    ) = {
      val diff = val2 - val1
      diff >= minCoorDiff
    }

    def sortBy(
      minCoorDiff: Double
    )(
      coor: BoundingBox => Double
    )(
      bboxValues: Seq[(BoundingBox, T)]
    ) =
      if (bboxValues.size > 1)
        sortIntoGroup(
          (
            bbox1: BoundingBox,
            bbox2: BoundingBox
          ) => isSmallerAux(minCoorDiff)(coor(bbox1), coor(bbox2))
        )(bboxValues)
      else
        Seq(bboxValues)

    val results = for {
      group1 <- sortBy(minYCoorDiff)(_.minY)(bboxExts)

      group2 <- sortBy(minXCoorDiff)(_.minX)(group1)

      group3 <- sortBy(minYCoorDiff)(_.maxY)(group2)

      group4 <- sortBy(minXCoorDiff)(_.maxX)(group3)
    } yield group4

    results.flatten
  }

  private def sortAux[T](
    isXSmaller: (Double, Double) => Boolean,
    isYSmaller: (Double, Double) => Boolean,
    isXEqual: (Double, Double) => Boolean,
    isYEqual: (Double, Double) => Boolean
  )(
    bboxExts: Seq[(BoundingBox, T)]
  ) =
    bboxExts.sortWith { case ((box1, _), (box2, _)) =>
      val minYEq = isYEqual(box1.minY, box2.minY)
      val minXEq = isXEqual(box1.minX, box2.minX)
      val maxYEq = isYEqual(box1.maxY, box2.maxY)

      isYSmaller(box1.minY, box2.minY) ||
      (minYEq && isXSmaller(box1.minX, box2.minX)) ||
      (minYEq && minXEq && isYSmaller(box1.maxY, box2.maxY)) ||
      (minYEq && minXEq && maxYEq && isXSmaller(box1.maxX, box2.maxX))
    }

  private def sortIntoGroup[T](
    isSmaller: (BoundingBox, BoundingBox) => Boolean
  )(
    bboxExts: Seq[(BoundingBox, T)]
  ) = {
    val sorted1 = bboxExts.sortWith { case ((box1, _), (box2, _)) => isSmaller(box1, box2) }

    // split into "equals" parts
    sorted1.tail.foldLeft(Seq(Seq(sorted1.head))) { case (acc, (box, el)) =>
      val lastGroup = acc.last
      val lastBox = lastGroup.head._1
      if (!isSmaller(lastBox, box) && !isSmaller(lastBox, box))
        acc.init :+ (lastGroup :+ (box, el))
      else
        acc :+ Seq((box, el))
    }
  }

  protected def pageContentToTextLines(
    pageContent: PageContent
  ): Seq[String] = {
    val elsSorted = pageContent.elements
    pageContent.elements.zipWithIndex.flatMap { case (PageElementExt(element, box), index) =>
      val content = element match {
        case e: TableElement =>
          Seq("") ++ e.content
        case e: TextElement =>
          Seq(e.content)
      }

      if (index > 0) {
        val prevLineBox = elsSorted(index - 1).box
        val prevLineHeight = Math.abs(prevLineBox.maxY - prevLineBox.minY)
        val lineHeight = Math.abs(box.maxY - box.minY)
        val avgLineHeight = (prevLineHeight + lineHeight) / 2

        // add a new line if a gap between lines is too big
        if (box.minY - prevLineBox.maxY > avgLineHeight) {
          Seq("") ++ content
        } else
          content
      } else
        content
    }
  }

  protected def pageContentToTextLinesSimple(
    pageContent: PageContent
  ): Seq[String] = {
    val els = pageContent.elements
    els.zipWithIndex.flatMap { case (PageElementExt(el, boundingBox), index) =>
      el match {
        case e: TableElement =>
          Seq("") ++ e.content
        case e: TextElement =>
          val yTop = boundingBox.minY
          val yBottom = boundingBox.maxY

          if (index > 0) {
            val prevElBBox = els(index - 1).box

            val prevYTop = prevElBBox.minY
            val prevYBottom = prevElBBox.maxY

            val prevLineHeight = Math.abs(prevYTop - prevYBottom)
            val lineHeight = Math.abs(yTop - yBottom)

            val avgLineHeight = (prevLineHeight + lineHeight) / 2

            // add a new line if a gap between lines is too big
            if (Math.abs(yTop - prevYBottom) > avgLineHeight)
              Seq("", e.content)
            else
              Seq(e.content)
          } else
            Seq(e.content)
      }
    }
  }

  private def extractPageContentsAux(
    layoutResult: LayoutAnalyzeResult,
    tableInfos: Seq[TableInfoAux],
    relaxedCentroidCheck: Boolean
  ): Seq[PageContent] =
    layoutResult.pages.zipWithIndex.map { case (page, pageIndex) =>
      val height = page.height
      val pageNumber = page.pageNumber
      val pageTableInfos = tableInfos.filter { tableInfo =>
        tableInfo.pageNumber == pageNumber
      }

      val els = page.lines.zipWithIndex.flatMap { case (line, index) =>
        // checks if it is a part of table
        pageTableInfos.find { tableInfo =>
          isPolygonInside(tableInfo.boundingPolygon, line.polygon, relaxedCentroidCheck)
        } match {
          case Some(tableThisLineBelongsTo) =>
            if (tableThisLineBelongsTo.minLineIndex == index) {
              val bbox = polygonToBoundingBox(tableThisLineBelongsTo.boundingPolygon)
              val el = TableElement(
                tableThisLineBelongsTo.tableNumber,
                tableThisLineBelongsTo.rowCount,
                tableThisLineBelongsTo.columnCount,
                tableThisLineBelongsTo.newLines
              )

              Some((bbox, el))
            } else
              None
          case None =>
            val bbox = polygonToBoundingBox(line.polygon)
            Some((bbox, TextElement(line.content)))
        }
      }

      val elements = els.map { case (boundingBox, el) =>
        PageElementExt(el, boundingBox)
      }

      PageContent(
        pageNumber = pageIndex + 1, // starting from 1
        width = page.width,
        height = page.height,
        elements
      )
    }

  private def extractPageContentsSortedAux(
    layoutResult: LayoutAnalyzeResult,
    tableInfos: Seq[TableInfoAux],
    relaxedCentroidCheck: Boolean,
    compareCoorRelativeToPage: Boolean = false,
    minCoorDiffPercent: Double,
    addWords: Boolean = false
  ): Seq[PageContent] = {
    val sort =
      if (compareCoorRelativeToPage)
        (page: LayoutPage) =>
          sortTopBottomLeftRightForPage[PageElement](
            _,
            page.width,
            page.height,
            minCoorDiffPercent
          )
      else
        (_: LayoutPage) => sortTopBottomLeftRight[PageElement](_, minCoorDiffPercent)

    extractPageContentsSortedCustomAux(sort)(
      layoutResult,
      tableInfos,
      relaxedCentroidCheck,
      addWords
    )
  }

  private def extractPageContentsSortedCustomAux(
    sortByBoundingBox: LayoutPage => Seq[(BoundingBox, PageElement)] => Seq[
      (BoundingBox, PageElement)
    ]
  )(
    layoutResult: LayoutAnalyzeResult,
    tableInfos: Seq[TableInfoAux],
    relaxedCentroidCheck: Boolean,
    addWords: Boolean = false
  ): Seq[PageContent] =
    // find table lines and replace them with enriched table content
    layoutResult.pages.zipWithIndex.map { case (page, pageIndex) =>
      val sortAux = sortByBoundingBox(page)

      val pageNumber = page.pageNumber
      val pageTableInfos = tableInfos.filter { tableInfo =>
        tableInfo.pageNumber == pageNumber
      }

      val remainingLines = page.lines.filterNot { line =>
        // checks if it is a part of table
        pageTableInfos.exists { tableInfo =>
          isPolygonInside(tableInfo.boundingPolygon, line.polygon, relaxedCentroidCheck)
        }
      }

      val elements1 = pageTableInfos.map { tableInfo =>
        val el = TableElement(
          tableInfo.tableNumber,
          tableInfo.rowCount,
          tableInfo.columnCount,
          tableInfo.newLines
        )
        val bbox = polygonToBoundingBox(tableInfo.boundingPolygon)
        (bbox, el)
      }

      val elements2 = remainingLines.map { line =>
        val el = TextElement(line.content)
        val bbox = polygonToBoundingBox(line.polygon)
        (bbox, el)
      }

      val elements = sortAux(elements1 ++ elements2).map { case (box, element) =>
        PageElementExt(element, box)
      }

      val words = page.words.map { word =>
        val bbox = polygonToBoundingBox(word.polygon)
        PageTextExt(TextElement(word.content), bbox)
      }

      PageContent(
        pageNumber = pageIndex + 1, // starting from 1
        width = page.width,
        height = page.height,
        elements,
        words = if (addWords) words else Nil
      )
    }

  protected def extractEnhancedContent(
    readResult: ReadAnalyzeResult,
    linesPerPageForNewLine: Option[Int] = None,
    sortParagraphsTopBottomLeftRight: Boolean = false,
    fixOverlapingParagraphs: Boolean = false,
    sortMinCoorDiffPercent: Option[Double] =
      None, // used only when fixOverlapingParagraphs = true
    withRotationCorrection: Boolean = true
  ): Seq[Seq[String]] = {
    val pageNumberParagraphs = readResult.paragraphs.zipWithIndex.map { case (par, parIndex) =>
      val pageNumber = par.boundingRegions.head.pageNumber
      (pageNumber, (par, parIndex))
    }.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))

    readResult.pages.map { page =>
      val height = page.height
      val pageNumber = page.pageNumber

      val newLineMinGap =
        height / linesPerPageForNewLine.getOrElse(
          ReadModelDefaults.linesPerPageForNewLineThreshold
        )

      val pageParsWithIndexes = pageNumberParagraphs.get(pageNumber).getOrElse(Nil).toSeq

      val pagePars = if (sortParagraphsTopBottomLeftRight) {
        val pageParsBBoxes = pageParsWithIndexes.map { case (par, _) =>
          val polygon = par.boundingRegions.head.polygon
          val bbox = polygonToBoundingBox(polygon)
          (bbox, par)
        }

        val sortMinCoorDiffPercentActual =
          sortMinCoorDiffPercent.getOrElse(
            ReadModelDefaults.paragraphFixSortMinCoorDiffPercent
          )

        sortTopBottomLeftRightForPage(
          pageParsBBoxes,
          page.width,
          page.height,
          sortMinCoorDiffPercentActual
        ).map(_._2)
      } else {
        // otherwise sort by the original order/indexes
        pageParsWithIndexes.sortBy(_._2).map(_._1)
      }

      // replace overlapping paragraphs with the new content based on words (if needed)
      val groupIndexFixedParsMap =
        if (fixOverlapingParagraphs)
          createGroupIndexFixedParsMap(
            page,
            pagePars,
            sortMinCoorDiffPercent,
            newLineMinGap / 2,
            withRotationCorrection
          )
        else
          Map.empty[Int, (Seq[Int], BoundingBox, String)]

      val parContentWithBBoxesFixed = pagePars.zipWithIndex.flatMap { case (par, parIndex) =>
        groupIndexFixedParsMap.get(parIndex) match {
          case Some((group, groupBBox, newContent)) =>
            // replace only if the first paragraph in the group
            if (group.head == parIndex) {
              Some((newContent, groupBBox))
            } else
              None

          case None =>
            val originalPolygon = toCoors(par.boundingRegions.head.polygon)

            val rotatedPolygon = if (withRotationCorrection) {
              rotatePolygon(
                originalPolygon,
                -page.angle, // Note the angle is negated for counter-clockwise rotation
                useCentroid = false // use (0, 0) as the rotation center
              )
            } else
              originalPolygon

            val rotatedBbox = polygonCoorsToBoundingBox(rotatedPolygon)

            Some((par.content, rotatedBbox))
        }
      }

      parContentWithBBoxesFixed.zipWithIndex.flatMap {
        case ((parContent, parBoundingBox), pageParIndex) =>
          if (pageParIndex > 0) {
            val (prevContent, prevParBoundingBox) = parContentWithBBoxesFixed(pageParIndex - 1)

            val prevYBottom = prevParBoundingBox.maxY
            val yTop = parBoundingBox.minY
            val yBottom = parBoundingBox.maxY

            // add a new line if a gap between lines is too big
            if (yTop - prevYBottom > newLineMinGap)
              Seq("", parContent)
            else {
              Seq(parContent)
            }
          } else
            Seq(parContent)
      }
    }
  }

  private def boundingBoxToString(boundingBox: BoundingBox) =
    s"(${boundingBox.minX}, ${boundingBox.minY}) - (${boundingBox.maxX}, ${boundingBox.maxY})"

  private def calcOuterExtPolygonWithOriginalBBox(
    paragraphPolygonCoors: Seq[Seq[(Double, Double)]]
  ) = {
    // TODO: use convex hull instead of bounding box conversion
    val parBBoxes = paragraphPolygonCoors.map(polygonCoorsToBoundingBox)

    val groupBBox = BoundingBox(
      minX = parBBoxes.map(_.minX).min,
      minY = parBBoxes.map(_.minY).min,
      maxX = parBBoxes.map(_.maxX).max,
      maxY = parBBoxes.map(_.maxY).max
    )

    val xLength = groupBBox.maxX - groupBBox.minX
    val yLength = groupBBox.maxY - groupBBox.minY

    val ratio =
      ReadModelDefaults.paragraphFixOuterBoundingBoxExtraPaddingPercent.toDouble / 100

    val expandedGroupBBox = BoundingBox(
      minX = groupBBox.minX - ratio * xLength,
      minY = groupBBox.minY - ratio * yLength,
      maxX = groupBBox.maxX + ratio * xLength,
      maxY = groupBBox.maxY + ratio * yLength
    )

    val extPol = toCoors(boundingBoxToPolygon(expandedGroupBBox))

    (extPol, groupBBox)
  }

  private def createGroupIndexFixedParsMap(
    page: Page,
    pagePars: Seq[Paragraph],
    sortMinCoorDiffPercent: Option[Double],
    newLineMinGap: Double,
    withRotationCorrection: Boolean
  ) = {
    def rotateAux(coors: Seq[(Double, Double)]) =
      if (withRotationCorrection) rotatePolygon(coors, -page.angle, useCentroid = false)
      else coors

    val paragraphPolygonCoors = pagePars.map { par =>
      val polygon = toCoors(par.boundingRegions.head.polygon)
      rotateAux(polygon)
    }

    findParagraphsOverlapContinuousGroups(paragraphPolygonCoors).flatMap { group =>
      val groupPolygonCoors = group.map { parIndex => paragraphPolygonCoors(parIndex) }
      val (groupExtPolygon, groupBBox) = calcOuterExtPolygonWithOriginalBBox(groupPolygonCoors)

      val groupPars = group.map(parIndex => pagePars(parIndex))
      val groupParsContents = groupPars.flatMap(_.content.split("\\s+")).toSet

      val wordsWithPolygons = page.words.map { word =>
        val polygon = rotateAux(toCoors(word.polygon))
        (word, polygon)
      }

      val inWordsWithPolygons = wordsWithPolygons.filter { case (word, polygon) =>
        val isInside = isPolygonInsideCoorPairs(
          groupExtPolygon,
          polygon,
          relaxedCentroidCheck = false
        )

        val isContained = groupParsContents.contains(word.content)

        isInside && isContained
      }

      val inBBoxWords = inWordsWithPolygons.map { case (word, polygon) =>
        (polygonCoorsToBoundingBox(polygon), word)
      }

      val sortMinCoorDiffPercentActual =
        sortMinCoorDiffPercent.getOrElse(ReadModelDefaults.paragraphFixSortMinCoorDiffPercent)

      val sortedWordsWithBBoxes = sortTopBottomLeftRightForPageInGroups(
        inBBoxWords,
        page.width,
        page.height,
        sortMinCoorDiffPercentActual
      )

      val wordsInLines = sortedWordsWithBBoxes.zipWithIndex.map {
        case ((bbox, word), wordIndex) =>
          if (wordIndex > 0) {
            val prevWordBBox = sortedWordsWithBBoxes(wordIndex - 1)._1
            if (
              (bbox.minY - prevWordBBox.minY > newLineMinGap) && (bbox.minX < prevWordBBox.minX)
            ) {
              "\n" + word.content
            } else
              word.content
          } else
            word.content
      }

      val newContent = wordsInLines.mkString(" ").replaceAll("\\s+\n", "\n").trim

      val groupParStrings = groupPars.zip(groupPolygonCoors).map { case (par, parCoors) =>
        val bbox = polygonCoorsToBoundingBox(parCoors)
        s"${boundingBoxToString(bbox)}: ${par.content}"
      }

      if (logParagraphsReplacement) {
        val wordStrings = sortedWordsWithBBoxes.map { case (bbox, word) =>
          s"${boundingBoxToString(bbox)}: ${word.content}"
        }

        logger.warn(
          s"""Replacing paragraphs ${group.mkString(", ")} on the page ${page.pageNumber}:
             |
             | ${groupParStrings.mkString("\n ")}
             |
             |resulting in:
             | ${newContent}""".stripMargin
        )
        //        with the words:
        //          ${wordStrings.mkString("\n ")}

      }

      group.map { parIndex =>
        (parIndex, (group, groupBBox, newContent))
      }
    }.toMap
  }

  private def findParagraphsOverlapContinuousGroups(
    paragraphPolygonCoors: Seq[Seq[(Double, Double)]],
    minYOverlapPercent: Double = ReadModelDefaults.minYOverlapPercent
  ) = {
    val groups = findParagraphsOverlapGroups(paragraphPolygonCoors, minYOverlapPercent)

    // split into groups on gaps
    groups.flatMap { group =>
      group.tail.foldLeft(Seq(Seq(group.head))) { case (acc, num) =>
        val lastGroup = acc.last
        val lastNum = lastGroup.last

        if (num - lastNum > 1)
          acc :+ Seq(num)
        else
          acc.init :+ (lastGroup :+ num)
      }
    }.filter(_.size > 1)
  }

  private def findParagraphsOverlapGroups(
    paragraphPolygonCoors: Seq[Seq[(Double, Double)]],
    minYOverlapPercent: Double = 3
  ) = {
    val overlaps = findParagraphsOverlaps(paragraphPolygonCoors, minYOverlapPercent)

    val parIndexConnectionsMap = overlaps.flatMap { case (index1, index2) =>
      Seq((index1, index2), (index2, index1))
    }.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))

    val visitedIndexes = Array.fill(paragraphPolygonCoors.size)(-1)

    def visit(
      parIndexes: Traversable[Int],
      groupIndex: Int
    ): Unit = {
      parIndexes.foreach { parIndex =>
        if (visitedIndexes(parIndex) == -1) {
          visitedIndexes(parIndex) = groupIndex
          val connections = parIndexConnectionsMap.get(parIndex).getOrElse(Nil)
          visit(connections, groupIndex)
        }
      }
    }

    (0 until paragraphPolygonCoors.size).foreach { parIndex =>
      visit(Seq(parIndex), parIndex)
    }

    visitedIndexes.zipWithIndex
      .groupBy(_._1)
      .map { case (groupIndex, indexes) =>
        (groupIndex, indexes.map(_._2).sorted)
      }
      .toSeq
      .sortBy(_._1)
      .filter(_._2.size > 1)
      .map(_._2)
  }

  private def findParagraphsOverlaps(
    paragraphPolygonCoors: Seq[Seq[(Double, Double)]],
    minYOverlapPercent: Double = 3
  ) = {
    paragraphPolygonCoors.zipWithIndex.flatMap { case (polygon1Coors, index1) =>
      val polygon1Ys = polygon1Coors.map(_._2)

      val polygon1MinY = polygon1Ys.min
      val polygon1MaxY = polygon1Ys.max

      paragraphPolygonCoors.zipWithIndex.flatMap { case (polygon2Coors, index2) =>
        if (index1 != index2) {
          val polygon2Ys = polygon2Coors.map(_._2)

          val polygon2MinY = polygon2Ys.min
          val polygon2MaxY = polygon2Ys.max

          val polygon2YOverlap =
            if (polygon1MaxY <= polygon2MinY || polygon2MaxY <= polygon1MinY) {
              0d
            } else {
              val minY = Math.max(polygon1MinY, polygon2MinY)
              val maxX = Math.min(polygon1MaxY, polygon2MaxY)
              (maxX - minY) / (polygon2MaxY - polygon2MinY)
            }

          val polygon2OverlapSufficiently = (polygon2YOverlap * 100) > minYOverlapPercent

          val isPolygon2Inside = polygon2Coors.exists(isInside(polygon1Coors, _))

          if (
            polygon2OverlapSufficiently && (polygonsIntersect(
              polygon1Coors,
              polygon2Coors
            ) || isPolygon2Inside)
          ) {
            Some((index1, index2))
          } else
            None
        } else
          None
      }
    }
  }

  protected def extractPageContentsSorted(
    layoutAnalyzeResult: LayoutAnalyzeResult,
    relaxedCentroidCheck: Boolean = false,
    compareCoorRelativeToPage: Boolean = false,
    minCoorDiffPercent: Double = 0.1,
    ignoreTables: Boolean = false,
    addWords: Boolean = false,
    filterTables: Option[TableInfoAux => Boolean] = None
  ): Seq[PageContent] = {
    val tableInfos =
      if (ignoreTables)
        Nil
      else
        extractTableInfos(layoutAnalyzeResult, relaxedCentroidCheck)

    val filteredTableInfos = filterTables match {
      case Some(filter) => tableInfos.filter(filter)
      case None => tableInfos
    }

    extractPageContentsSortedAux(
      layoutAnalyzeResult,
      filteredTableInfos,
      relaxedCentroidCheck,
      compareCoorRelativeToPage,
      minCoorDiffPercent,
      addWords
    )
  }

  protected def extractPageContents(
    layoutAnalyzeResult: LayoutAnalyzeResult,
    relaxedCentroidCheck: Boolean = false,
    ignoreTables: Boolean = false,
    filterTables: Option[TableInfoAux => Boolean] = None
  ): Seq[PageContent] = {
    val tableInfos =
      if (ignoreTables)
        Nil
      else
        extractTableInfos(layoutAnalyzeResult, relaxedCentroidCheck)

    val filteredTableInfos = filterTables match {
      case Some(filter) => tableInfos.filter(filter)
      case None => tableInfos
    }

    extractPageContentsAux(layoutAnalyzeResult, filteredTableInfos, relaxedCentroidCheck)
  }

  protected def extractTableInfos(
    layoutAnalyzeResult: LayoutAnalyzeResult,
    relaxedCentroidCheck: Boolean
  ): Seq[TableInfoAux] = {
    val pageNumberMap = layoutAnalyzeResult.pages.map(page => (page.pageNumber, page)).toMap

    layoutAnalyzeResult.tables.zipWithIndex.flatMap { case (table, tableIndex) =>
      val boundingRegion = table.boundingRegions.head
      val pageNumber = boundingRegion.pageNumber
      val page = pageNumberMap
        .get(pageNumber)
        .getOrElse(
          throw new IllegalArgumentException(s"Page ${pageNumber} not found")
        )

      val tableLines = findTableLines(layoutAnalyzeResult, table, relaxedCentroidCheck)

      if (tableLines.nonEmpty) {
        validateTableLines(tableLines, page, table, tableIndex)

        val originalTableLineContents = tableLines.map(_._1.content)
        val newLines =
          tableToLines(table, originalTableLineContents, rowDelimiter = Some("--"))

        val minLineIndex = tableLines.map(_._2).min

        val infoAux = TableInfoAux(
          pageNumber,
          page.width,
          page.height,
          boundingRegion.polygon,
          tableIndex + 1, // starting from 1
          table.rowCount,
          table.columnCount,
          minLineIndex,
          newLines
        )

        Some(infoAux)
      } else {
        logger.warn(s"Table ${tableIndex} at the page ${pageNumber} has no lines.")
        None
      }
    }
  }

  protected def validateTableLines(
    tableLines: Seq[(Line, Int)],
    page: LayoutPage,
    table: Table,
    tableIndex: Int
  ) = {
    if (tableLines.isEmpty)
      throw new IllegalArgumentException(s"Table ${tableIndex} has no lines")

    val tablePageNumber = table.boundingRegions.head.pageNumber

    val pageLines = page.lines

    // validate if there are no gaps between table lines
    val tableLinesIndexes = tableLines.map(_._2)
    val tableLinesIndexesRange = tableLinesIndexes.head to tableLinesIndexes.last
    val tableLinesIndexesRangeDiff = tableLinesIndexesRange.diff(tableLinesIndexes)
    if (tableLinesIndexesRangeDiff.nonEmpty) {
      val diffContents = pageLines.zipWithIndex.filter { case (_, index) =>
        tableLinesIndexesRangeDiff.contains(index)
      }.map(_._1.content)

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
    val tableCellsString = tableToLinesContent(table, originalTableLineContents, None)
      .mkString(" ")
      .replaceAll("\\s+", " ")
      .trim
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
  }

  // find table lines
  protected def findTableLines(
    layoutAnalyzeResult: LayoutAnalyzeResult,
    table: Table,
    relaxedCentroidCheck: Boolean
  ) = {
    val tablePageNumber = table.boundingRegions.head.pageNumber
    val tablePolygon = table.boundingRegions.head.polygon
    val tablePolygonCoordinates = toCoors(tablePolygon)

    val tablePage = layoutAnalyzeResult.pages
      .find(_.pageNumber == tablePageNumber)
      .getOrElse(
        throw new IllegalArgumentException(s"Page ${tablePageNumber} not found")
      )

    tablePage.lines.zipWithIndex.filter { case (line, _) =>
      val linePolygon = toCoors(line.polygon)
      isPolygonInsideCoorPairs(tablePolygonCoordinates, linePolygon, relaxedCentroidCheck)
    }
  }

  private def tableToLines(
    table: Table,
    originalTableLines: Seq[String],
    rowDelimiter: Option[String]
  ) = {
    val cellLines = tableToLinesContent(table, originalTableLines, rowDelimiter)

    Seq(
      s"Table ${table.rowCount} (rows) x ${table.columnCount} (columns):",
      "--"
    ) ++ cellLines
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

    var remainingOriginalTableCellContent =
      originalTableCells.mkString(" ").replaceAll("\\s+", " ").trim

    (0 until table.rowCount).flatMap { rowIndex =>
      (0 until table.columnCount).map { columnIndex =>
        rowColumnIndexCellMap
          .get((rowIndex, columnIndex))
          .map { cell =>
            // fix weird values from line above or bellow that occur once a while
            val content = if (cell.content.contains("\n")) {
              val options = cell.content.split("\n").map(_.trim)
              options
                .find(remainingOriginalTableCellContent.startsWith)
                .getOrElse(options.head)
            } else {
              cell.content.trim
            }

            remainingOriginalTableCellContent =
              remainingOriginalTableCellContent.stripPrefix(content).trim

            content
          }
          .getOrElse(
            ""
          )
      } ++ (afterRowLine.map(Seq(_)).getOrElse(Nil))
    }
  }

  protected case class TableInfoAux(
    pageNumber: Int,
    pageWidth: Double,
    pageHeight: Double,
    boundingPolygon: Seq[Double],
    tableNumber: Int,
    rowCount: Int,
    columnCount: Int,
    minLineIndex: Int,
    newLines: Seq[String]
  )
}
