package io.cequence.azureform.service

import io.cequence.azureform.AzureFormRecognizerClientException
import play.api.libs.json._

import java.util.Date
import java.{util => ju}

object JsonUtil {

  implicit class JsonOps(val json: JsValue) {
    def asSafe[T](
      implicit fjs: Reads[T]
    ): T =
      try {
        json.validate[T] match {
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            val errorString = errors.map { case (path, pathErrors) =>
              s"JSON at path '${path}' contains the following errors: ${pathErrors.map(_.message).mkString(";")}"
            }.mkString("\n")
            throw new AzureFormRecognizerClientException(
              s"Unexpected JSON:\n'${Json.prettyPrint(json)}'. Cannot be parsed due to: $errorString"
            )
        }
      } catch {
        case e: Exception =>
          throw new AzureFormRecognizerClientException(
            s"Error thrown while processing a JSON '$json'. Cause: ${e.getMessage}"
          )
      }

    def asSafeArray[T](
      implicit fjs: Reads[T]
    ): Seq[T] =
      json.asSafe[JsArray].value.map(_.asSafe[T]).toSeq
  }

  object SecDateFormat extends Format[ju.Date] {
    override def reads(json: JsValue): JsResult[Date] = {
      json match {
        case JsString(s) =>
          try {
            val millis = s.toLong * 1000
            JsSuccess(new ju.Date(millis))
          } catch {
            case _: NumberFormatException => JsError(s"$s is not a number.")
          }

        case JsNumber(n) =>
          val millis = (n * 1000).toLong
          JsSuccess(new ju.Date(millis))

        case _ => JsError(s"String or number expected but got '$json'.")
      }
    }

    override def writes(o: Date): JsValue =
      JsNumber(Math.round(o.getTime.toDouble / 1000))
  }

  def toJson(value: Any): JsValue =
    if (value == null)
      JsNull
    else
      value match {
        case x: JsValue    => x // nothing to do
        case x: String     => JsString(x)
        case x: BigDecimal => JsNumber(x)
        case x: Integer    => JsNumber(BigDecimal.valueOf(x.toLong))
        case x: Long       => JsNumber(BigDecimal.valueOf(x))
        case x: Double     => JsNumber(BigDecimal.valueOf(x))
        case x: Float      => JsNumber(BigDecimal.valueOf(x.toDouble))
        case x: Boolean    => JsBoolean(x)
        case x: ju.Date    => Json.toJson(x)
        case x: Option[_]  => x.map(toJson).getOrElse(JsNull)
        case x: Array[_]   => JsArray(x.map(toJson))
        case x: Seq[_]     => JsArray(x.map(toJson))
        case x: Map[String, _] =>
          val jsonValues = x.map { case (fieldName, value) =>
            (fieldName, toJson(value))
          }
          JsObject(jsonValues)
        case _ =>
          throw new IllegalArgumentException(
            s"No JSON formatter found for the class ${value.getClass.getName}."
          )
      }
}
