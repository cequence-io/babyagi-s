package io.cequence.azureform.model

case class AzureInvoiceResponse(
  status: String,
  createdDateTime: String,
  lastUpdatedDateTime: String,
  analyzeResult: Option[InvoiceAnalyzeResult]
) extends HasStatus

case class InvoiceAnalyzeResult(
  tables: Seq[Table],
  apiVersion: String,
  pages: Seq[Page],
  keyValuePairs: Seq[KeyValuePair],
  modelId: String,
  documents: Seq[Document],
  stringIndexType: String,
  paragraphs: Seq[Paragraph],
  content: String
)

case class KeyValuePair(
  confidence: Double,
  value: Option[Value],
  key: Key
)

case class Value(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  content: String
)

case class Key(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  content: String
)

case class Document(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  docType: String,
  confidence: Double,
  fields: InvoiceFields
)

case class InvoiceFields(
  ServiceAddress: Option[ValueAddressEntry],
  ServiceAddressRecipient: Option[ValueStringEntry],
  PreviousUnpaidBalance: Option[ValueCurrencyEntry],
  RemittanceAddressRecipient: Option[ValueStringEntry],
  InvoiceId: Option[ValueStringEntry],
  SubTotal: Option[ValueCurrencyEntry],
  BillingAddress: Option[ValueAddressEntry],
  TotalTax: Option[ValueCurrencyEntry],
  ServiceStartDate: Option[ValueDateEntry],
  Items: Option[Items],
  CustomerName: Option[ValueStringEntry],
  InvoiceDate: Option[ValueDateEntry],
  DueDate: Option[ValueDateEntry],
  CustomerAddressRecipient: Option[ValueStringEntry],
  RemittanceAddress: Option[ValueAddressEntry],
  AmountDue: Option[ValueCurrencyEntry],
  VendorName: Option[ValueStringEntry],
  ServiceEndDate: Option[ValueDateEntry],
  CustomerId: Option[ValueStringEntry],
  VendorAddressRecipient: Option[ValueStringEntry],
  ShippingAddressRecipient: Option[ValueStringEntry],
  InvoiceTotal: Option[ValueCurrencyEntry],
  ShippingAddress: Option[ValueAddressEntry],
  BillingAddressRecipient: Option[ValueStringEntry],
  PurchaseOrder: Option[ValueStringEntry],
  VendorAddress: Option[ValueAddressEntry],
  CustomerAddress: Option[ValueAddressEntry]
)

case class ValueAddressEntry(
  boundingRegions: Seq[BoundingRegion],
  valueAddress: ValueAddress,
  spans: Seq[Span],
  confidence: Double,
  `type`: String,
  content: String
) extends IsContentEntry

case class ValueAddress(
  road: Option[String],
  city: Option[String],
  streetAddress: Option[String],
  postalCode: Option[String],
  houseNumber: Option[String],
  state: Option[String]
)

case class ValueStringEntry(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  valueString: String,
  confidence: Double,
  `type`: String,
  content: String
) extends IsContentEntry

case class ValueCurrencyEntry(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  valueCurrency: ValueCurrency,
  confidence: Double,
  `type`: String,
  content: String
) extends IsContentEntry

trait IsContentEntry {
  def content: String
}

case class ValueCurrency(
  amount: Double,
  currencySymbol: Option[String]
)

case class Items(
  valueArray: Seq[ValueArray],
  `type`: String
)

case class ValueArray(
  boundingRegions: Seq[BoundingRegion],
  valueObject: ValueObject,
  spans: Seq[Span],
  confidence: Double,
  `type`: String,
  content: String
)

case class ValueObject(
  UnitPrice: Option[ValueCurrencyEntry],
  Description: Option[ValueStringEntry],
  ProductCode: Option[ValueStringEntry],
  Amount: Option[ValueCurrencyEntry],
  Quantity: Option[Quantity],
  Tax: Option[ValueCurrencyEntry],
  Unit: Option[ValueStringEntry],
  Date: Option[ValueDateEntry]
)

case class Quantity(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  valueNumber: Double,
  confidence: Double,
  `type`: String,
  content: String
)

case class ValueDateEntry(
  boundingRegions: Seq[BoundingRegion],
  spans: Seq[Span],
  confidence: Double,
  valueDate: String,
  `type`: String,
  content: String
) extends IsContentEntry
