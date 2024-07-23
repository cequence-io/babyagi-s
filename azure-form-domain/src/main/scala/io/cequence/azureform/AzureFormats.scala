package io.cequence.azureform

import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import io.cequence.azureform.model._
import io.cequence.wsclient.JsonUtil.enumFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

object AzureFormats {
  implicit val spanFormat = Json.format[Span]
  implicit val wordFormat = Json.format[Word]
  implicit val lineFormat = Json.format[Line]
  implicit val languageFormat = Json.format[Language]
  implicit val boundingRegionFormat = Json.format[BoundingRegion]

  implicit val paragraphRoleFormat: Format[ParagraphRole] = enumFormat(
    ParagraphRole.title,
    ParagraphRole.sectionHeading,
    ParagraphRole.footnote,
    ParagraphRole.pageHeader,
    ParagraphRole.pageFooter,
    ParagraphRole.pageNumber
  )

  implicit val paragraphFormat = Json.format[Paragraph]
  implicit val pageFormat = Json.format[Page]

  implicit val selectionMarkFormat = Json.format[SelectionMark]
  implicit val layoutPageFormat = Json.format[LayoutPage]

  implicit val cellFormat = Json.format[Cell]
  implicit val tableFormat = Json.format[Table]

  implicit val valueFormat = Json.format[Value]
  implicit val keyFormat = Json.format[Key]
  implicit val keyValuePairFormat = Json.format[KeyValuePair]
  implicit val valueAddressFormat = Json.format[ValueAddress]
  implicit val valueAddressEntryFormat = Json.format[ValueAddressEntry]
  implicit val valueStringEntryFormat = Json.format[ValueStringEntry]
  implicit val valueCurrencyFormat = Json.format[ValueCurrency]
  implicit val valueCurrencyEntryFormat = Json.format[ValueCurrencyEntry]
  implicit val valueDateEntryFormat = Json.format[ValueDateEntry]
  implicit val quantityFormat = Json.format[Quantity]
  implicit val valueObjectFormat = Json.format[ValueObject]
  implicit val valueArrayFormat = Json.format[ValueArray]
  implicit val itemsFormat = Json.format[Items]
  implicit val fieldsFormat = Jsonx.formatCaseClass[InvoiceFields]
  implicit val documentFormat = Json.format[Document]

  implicit val contentFormatFormat: Format[ContentFormat] = enumFormat(
    ContentFormat.markdown,
    ContentFormat.text
  )

  implicit val readAnalyzeResultFormat: Format[ReadAnalyzeResult] = {
    val reads: Reads[ReadAnalyzeResult] = (
      (__ \ "apiVersion").read[String] and
        (__ \ "modelId").read[String] and
        (__ \ "stringIndexType").read[String] and
        (__ \ "content").read[String] and
        (__ \ "pages").read[Seq[Page]].orElse(Reads.pure(Nil)) and
        (__ \ "paragraphs").read[Seq[Paragraph]].orElse(Reads.pure(Nil)) and
        (__ \ "languages").read[Seq[Language]].orElse(Reads.pure(Nil))
    )(ReadAnalyzeResult.apply _)

    val writes: Writes[ReadAnalyzeResult] = Json.writes[ReadAnalyzeResult]
    Format(reads, writes)
  }

  implicit val azureReadResponseFormat = Json.format[AzureReadResponse]

  implicit val layoutAnalyzeResultFormat: Format[LayoutAnalyzeResult] = {
    val reads: Reads[LayoutAnalyzeResult] = (
      (__ \ "apiVersion").read[String] and
        (__ \ "modelId").read[String] and
        (__ \ "stringIndexType").read[String] and
        (__ \ "content").read[String] and
        (__ \ "pages").read[Seq[LayoutPage]].orElse(Reads.pure(Nil)) and
        (__ \ "paragraphs").read[Seq[Paragraph]].orElse(Reads.pure(Nil)) and
        (__ \ "tables").read[Seq[Table]].orElse(Reads.pure(Nil)) and
        (__ \ "languages").read[Seq[Language]].orElse(Reads.pure(Nil)) and
        (__ \ "contentFormat").readNullable[ContentFormat]
    )(LayoutAnalyzeResult.apply _)

    val writes: Writes[LayoutAnalyzeResult] = Json.writes[LayoutAnalyzeResult]
    Format(reads, writes)
  }

  implicit val azureLayoutResponseFormat = Json.format[AzureLayoutResponse]
  implicit val invoiceAnalyzeResultFormat = Json.format[InvoiceAnalyzeResult]
  implicit val azureInvoiceResponseFormat = Json.format[AzureInvoiceResponse]
}
