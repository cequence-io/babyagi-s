package io.cequence.azureform.service

import io.cequence.azureform.model.BoundingBox
import org.kynosarges.tektosyne.geometry.{GeoUtils, LineD, PointD, PolygonLocation}

import scala.math.{cos, sin, toRadians}

trait PolygonHelper {

  protected def isPolygonInside(
    outerPolygon: Seq[Double],
    polygon: Seq[Double],
    relaxedCentroidCheck: Boolean
  ): Boolean = {
    val outerPolygonCoors = toCoors(outerPolygon)
    val polygonCoors = toCoors(polygon)

    isPolygonInsideCoorPairs(outerPolygonCoors, polygonCoors, relaxedCentroidCheck)
  }

  protected def isPolygonInsideCoorPairs(
    outerPolygon: Seq[(Double, Double)],
    polygon: Seq[(Double, Double)],
    relaxedCentroidCheck: Boolean
  ): Boolean =
    if (relaxedCentroidCheck)
      isPolygonInsideCoorPairsWithCentroid(outerPolygon, polygon)
    else
      polygon.forall(isInside(outerPolygon, _))

  protected def isPolygonInsideCoorPairsWithCentroid(
    outerPolygon: Seq[(Double, Double)],
    polygon: Seq[(Double, Double)],
    minPointsInThreshold: Double = 0.5
  ): Boolean = {
    val count = polygon.count(isInside(outerPolygon, _))

    val centroid = GeoUtils.polygonCentroid(polygon.map { case (x, y) =>
      new PointD(x, y)
    }: _*)
    val isCentroidInside = isInside(outerPolygon, (centroid.x, centroid.y))

    (count.toDouble / polygon.size >= minPointsInThreshold && isCentroidInside)
  }

  protected def toCoors(polygon: Seq[Double]) =
    polygon.grouped(2).map { case Seq(x, y) => (x, y) }.toSeq

  protected def isInside(
    polygon: Seq[(Double, Double)],
    point: (Double, Double)
  ): Boolean = {
    val polygonPoints = polygon.map { case (x, y) => new PointD(x, y) }
    val polygonLocation =
      GeoUtils.pointInPolygon(new PointD(point._1, point._2), polygonPoints.toArray)

    polygonLocation != PolygonLocation.OUTSIDE
  }

  protected def polygonsIntersect(
    polygon1: Seq[(Double, Double)],
    polygon2: Seq[(Double, Double)]
  ): Boolean = {
    def toLines(pol: Seq[(Double, Double)]) = pol.zip(pol.tail :+ pol.head).map {
      case (p1, p2) => new LineD(p1._1, p1._2, p2._1, p2._2)
    }

    val lines1 = toLines(polygon1)
    val lines2 = toLines(polygon2)

    lines1.exists(line1 => lines2.exists(line2 => line1.intersect(line2).exists()))
  }

  protected def polygonHeight(
    polygon: Seq[Double]
  ): Double = {
    val ys = toCoors(polygon).map(_._2)
    ys.max - ys.min
  }

  protected def polygonToBoundingBox(
    polygon: Seq[Double]
  ): BoundingBox =
    polygonCoorsToBoundingBox(toCoors(polygon))

  protected def polygonCoorsToBoundingBox(
    coors: Seq[(Double, Double)]
  ): BoundingBox = {
    val xs = coors.map(_._1)
    val ys = coors.map(_._2)

    BoundingBox(xs.min, ys.min, xs.max, ys.max)
  }

  protected def boundingBoxToPolygon(
    box: BoundingBox
  ) =
    Seq(box.minX, box.minY, box.maxX, box.minY, box.maxX, box.maxY, box.minX, box.maxY)

  protected def rotatePolygon(
    polygon: Seq[(Double, Double)],
    angleDegrees: Double,
    useCentroid: Boolean = true
  ) = {
    def translatePolygonAux(
      polygonPoints: Seq[PointD],
      translation: PointD
    ): Seq[PointD] =
      polygonPoints.map(p => new PointD(p.x - translation.x, p.y - translation.y))

    val polygonPoints = polygon.map { case (x, y) => new PointD(x, y) }

    val finalPolygonPoints = if (useCentroid) {
      val centroid = GeoUtils.polygonCentroid(polygonPoints: _*)

      val translatedPolygon = translatePolygonAux(polygonPoints, centroid)
      val rotatedPolygon = translatedPolygon.map(rotatePoint(_, angleDegrees))

      translatePolygonAux(rotatedPolygon, new PointD(-centroid.x, -centroid.y))
    } else {
      polygonPoints.map(rotatePoint(_, angleDegrees))
    }

    finalPolygonPoints.map(p => (p.x, p.y))
  }

  protected def rotatePoint(
    point: PointD,
    angleDegrees: Double
  ): PointD = {
    val angleRadians = toRadians(angleDegrees)
    val cosTheta = cos(angleRadians)
    val sinTheta = sin(angleRadians)

    new PointD(
      point.x * cosTheta + point.y * sinTheta,
      point.x * sinTheta + point.y * cosTheta
    )
  }
}
