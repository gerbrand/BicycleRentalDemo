package com.bicyclerental.domain

import java.util.{Currency, Locale}

import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives.complete
import org.threeten.extra.Interval
class BicycleRentalReporting(bicycleRentalBusiness: BicycleBusiness) {
  import BicycleRentalReporting._

  /**
   * Returns a csv containing invoice-rows for all sessions
   * that *start* (but not necessarily end) in the given interval.
   */
  def cyclistReport(cyclist: Cyclist, interval: Interval): Either[String, String] = {

    bicycleRentalBusiness.invoiceRows(cyclist, interval).map(invoiceRows => {
      val invoiceCsvRows: Iterable[Seq[String]] = invoiceRows.map(toLine(_))

      Seq(header).concat(invoiceCsvRows).map(toCsvRow).mkString(csvLineDelimiter)
    })


  }
}

object BicycleRentalReporting {
  val csvDelimiter = "|"
  val csvLineDelimiter = "\n"

  def toCsvRow(fields: Iterable[String]) = {
    fields.mkString(csvDelimiter)
  }

  import java.text.NumberFormat

  val defaultLocale = Locale.forLanguageTag("us")
  val euro = Currency.getInstance("EUR")

  val percentageFormat: NumberFormat = NumberFormat.getPercentInstance
  val monetaryFormat: NumberFormat = NumberFormat.getCurrencyInstance(defaultLocale)
  monetaryFormat.setCurrency(euro)

  val header = Seq("start", "end", "rentalRate (€/hour)", "serviceFee (%)", "totalPrice (€)", "totalServiceFee (€)")

  def toLine(invoiceRow: InvoiceRow): Seq[String] = {
    Seq(invoiceRow.start.toString,  invoiceRow.end.toString, monetaryFormat.format(invoiceRow.rentalFee), percentageFormat.format(invoiceRow.serviceFee), monetaryFormat.format(invoiceRow.totalPrice), monetaryFormat.format(invoiceRow.totalServiceFee) )
  }
}
