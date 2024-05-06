package io.cequence.azureform

import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import io.cequence.azureform.model._
import play.api.libs.json.Json

object AzureFormats {
  implicit val spanFormat = Json.format[Span]
  implicit val wordFormat = Json.format[Word]
  implicit val lineFormat = Json.format[Line]
  implicit val languageFormat = Json.format[Language]
  implicit val boundingRegionFormat = Json.format[BoundingRegion]
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

  implicit val readAnalyzeResultFormat = Json.format[ReadAnalyzeResult]
  implicit val azureReadResponseFormat = Json.format[AzureReadResponse]

  implicit val layoutAnalyzeResultFormat = Json.format[LayoutAnalyzeResult]
  implicit val azureLayoutResponseFormat = Json.format[AzureLayoutResponse]

  implicit val invoiceAnalyzeResultFormat = Json.format[InvoiceAnalyzeResult]
  implicit val azureInvoiceResponseFormat = Json.format[AzureInvoiceResponse]
}