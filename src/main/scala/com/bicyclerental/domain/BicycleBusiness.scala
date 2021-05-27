package com.bicyclerental.domain

import java.time.Instant

import akka.event.slf4j.SLF4JLogging
import com.bicyclerental.persistence.{RateRepository, RentalRepository}
import org.threeten.extra.Interval
import cats._, cats.data._, cats.implicits._
import scala.collection.SortedSet

/**
 * Business-logic regarding Bicycle Rentals
 */
class BicycleBusiness(rateReposistory: RateRepository, rentalRepository: RentalRepository) extends SLF4JLogging {
  import BicycleBusiness._
  def addRate(rateType: RateType, startTime: Instant, amount: BigDecimal ): Either[String, Rate] = addRate(Instant.now())(Rate(rateType, startTime, amount))
  def addRate(now: Instant)(rate: Rate ): Either[String, Rate] = {
    for {
      rate <- Rate.valid(rate)
      _ <- validToAdd(now, rate)
      _ <- Right(rateReposistory.addRate(rate))
    } yield rate
  }

  def findRate(rateType: RateType, time: Instant): Option[Rate] = {
    rateReposistory.findRate(rateType, time)
  }
  def findRates(rateType: RateType, interval: Interval): List[Rate] = {
    rateReposistory.findFees(rateType, interval)
  }

  def allRates(): Iterable[Rate] = rateReposistory.allRates

  def addRental: BikeRental => Either[String, Unit] = addRental(Instant.now())

  def addRental(now: Instant)(bicycleRental: BikeRental): Either[String, Unit] = {
    //Using either instead of require, because the latter throws an exception
    for {
      _ <- Either.cond(! bicycleRental.interval.getEnd.isAfter(now), (), "Rental can not start or end in the future")
      - <- Right(rentalRepository.add(bicycleRental))
    } yield()
  }

  def findRentals(interval: Interval): Iterable[BikeRental] = {
    rentalRepository.findByInterval(interval)
  }

  def findSessions(cyclist: Cyclist, interval: Interval): Iterable[BikeRental] = {
    rentalRepository.find(cyclist, interval)
  }

  def allInvoiceRows(interval: Interval): Either[String, Vector[InvoiceRow]] = {
    // Returns invoice-rows, returns Left(error) on first error
    import cats.implicits._
    val rowsPerSession: Either[String, Vector[Vector[InvoiceRow]]] = findRentals(interval).toVector.map(invoiceRowsPerSession).sequence

    rowsPerSession.map(_.flatten)
  }
  def invoiceRows(cyclist: Cyclist, interval: Interval): Either[String, Vector[InvoiceRow]] = {
    // Returns invoice-rows, returns Left(error) on first error
    import cats.implicits._
    val rowsPerSession: Either[String, Vector[Vector[InvoiceRow]]] = findSessions(cyclist, interval).toVector.map(invoiceRowsPerSession).sequence

    rowsPerSession.map(_.flatten)
  }

  /**
   * A rate is made up of two components (rates):
   * <ul>
    *   <li>a rental rate (expressed in currency / hour, e.g € / hour)
   *   <li>a service rate (given as a number between 0.0 (exclusive) and 0.5 (inclusive))
   * </ul>
   *
   * Since a rate may start or end during a bicycleRental, one bicycleRental
   * can result in multiple invoicerows.
   *
   */
  def invoiceRowsPerSession(bicycleRental: BikeRental): Either[String, Vector[InvoiceRow]] = {
    val interval = bicycleRental.interval
    val rentalFees = findRates(RateType.Renting, interval)
    val serviceFees = findRates(RateType.Service, interval)

    if (serviceFees.isEmpty) {
      Left("No service-rates known in interval " + bicycleRental.interval)
    } else {
      Right(calculateInvoiceRows(rentalFees, serviceFees, bicycleRental: BikeRental))
    }
  }
}

object BicycleBusiness {

  val msPerHour = 60*60*1000

  /**
   * The actual calculation of a bicycle-rental. All rates that are within the interval of the given bicycleRental
   * have to be supplied. Any rates that do not apply are ignored.
   */
  def calculateInvoiceRows(rentalFees: List[Rate], serviceFees: List[Rate], bicycleRental: BikeRental): Vector[InvoiceRow] = {
    val interval = bicycleRental.interval
    val feeStarts: Vector[Instant] = rentalFees.map(_.start).toVector
    val intervals = feeStarts.foldLeft(Vector(interval)) { case (intervals, time) => intervals.flatMap(splitInterval(time)) }
      .filter(_.toDuration.toMillis>0)

    intervals.map(rowInterval => {
      val start = rowInterval.getStart
      val end = rowInterval.getEnd
      val rentalFee: BigDecimal = Rate.find(rentalFees, RateType.Renting, start).map(_.amount).getOrElse(0)
      // We assume the serviceFee at the *end* of a rate-period is applicable.
      val serviceFee: BigDecimal = Rate.find(serviceFees, RateType.Service, end).map(_.amount).getOrElse(0)

      // Parking rate is calculated per ms. I assume no rounding to hour is needed.
      val totalPrice: BigDecimal = rentalFee / msPerHour * rowInterval.toDuration.toMillis
      val totalServiceFee: BigDecimal = totalPrice * serviceFee


      InvoiceRow(bicycleRental.cyclist, start = start, end = end, rentalFee: BigDecimal, serviceFee: BigDecimal, totalPrice: BigDecimal, totalServiceFee: BigDecimal)
    }).sortBy(_.start)
  }

  def splitInterval(time: Instant)(interval: Interval): Vector[Interval] = {
    if (interval.contains(time)) {
      Vector(Interval.of(interval.getStart, time), Interval.of(time, interval.getEnd))
    } else {
      Vector(interval)
    }
  }

  def validToAdd(now: Instant, rate: Rate ):Either[String, Rate] = {
    if (now.isAfter(rate.start)) {
      Left(s"Starttime ${rate.start} is in the past: before $now")
    } else {
      Right(rate)
    }
  }

}
