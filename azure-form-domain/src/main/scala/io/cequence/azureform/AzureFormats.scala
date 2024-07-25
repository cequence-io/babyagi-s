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
  // TODO: find a way to get rid of Jsonx.formatCaseClass (22+ fields)
  implicit val invoiceFieldsFormat = Jsonx.formatCaseClass[InvoiceFields]

//  implicit val invoiceFieldsFormat: Format[InvoiceFields] = (
//    (__ \ "ServiceAddress").formatNullable[ValueAddressEntry] and
//      (__ \ "ServiceAddressRecipient").formatNullable[ValueStringEntry] and
//      (__ \ "PreviousUnpaidBalance").formatNullable[ValueCurrencyEntry] and
//      (__ \ "RemittanceAddressRecipient").formatNullable[ValueStringEntry] and
//      (__ \ "InvoiceId").formatNullable[ValueStringEntry] and
//      (__ \ "SubTotal").formatNullable[ValueCurrencyEntry] and
//      (__ \ "BillingAddress").formatNullable[ValueAddressEntry] and
//      (__ \ "TotalTax").formatNullable[ValueCurrencyEntry] and
//      (__ \ "ServiceStartDate").formatNullable[ValueDateEntry] and
//      (__ \ "Items").formatNullable[Items] and
//      (__ \ "CustomerName").formatNullable[ValueStringEntry] and
//      (__ \ "InvoiceDate").formatNullable[ValueDateEntry] and
//      (__ \ "DueDate").formatNullable[ValueDateEntry] and
//      (__ \ "CustomerAddressRecipient").formatNullable[ValueStringEntry] and
//      (__ \ "RemittanceAddress").formatNullable[ValueAddressEntry] and
//      (__ \ "AmountDue").formatNullable[ValueCurrencyEntry] and
//      (__ \ "VendorName").formatNullable[ValueStringEntry] and
//      (__ \ "ServiceEndDate").formatNullable[ValueDateEntry] and
//      (__ \ "CustomerId").formatNullable[ValueStringEntry] and
//      (__ \ "VendorAddressRecipient").formatNullable[ValueStringEntry] and
//      (__ \ "ShippingAddressRecipient").formatNullable[ValueStringEntry] and
//      (__ \ "InvoiceTotal").formatNullable[ValueCurrencyEntry] and
//      (__ \ "ShippingAddress").formatNullable[ValueAddressEntry] and
//      (__ \ "BillingAddressRecipient").formatNullable[ValueStringEntry] and
//      (__ \ "PurchaseOrder").formatNullable[ValueStringEntry] and
//      (__ \ "VendorAddress").formatNullable[ValueAddressEntry] and
//      (__ \ "CustomerAddress").formatNullable[ValueAddressEntry]
//    )(InvoiceFields.apply, unlift(InvoiceFields.unapply))

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
