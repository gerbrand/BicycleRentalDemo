package com.bicyclerental

import java.time.{Duration, Instant}
import java.util.UUID

import com.bicyclerental.domain.RateType._
import com.bicyclerental.domain._
import com.bicyclerental.persistence.{RateRepository, RentalRepository}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.threeten.extra.Interval

class BicycleBusinessDomainSpec extends FunSpec with Matchers with BeforeAndAfter {

  private val rateRepository = new RateRepository
  private val rentalRepository = new RentalRepository
  val bicycleBusiness = new BicycleBusiness(rateRepository, rentalRepository)
  val reporting = new BicycleRentalReporting(bicycleBusiness)
  val now = Instant.parse("2019-11-27T19:22:50.127Z")

  after {
    rentalRepository.clear()
    rateRepository.clear()
  }

  describe("As BicycleRentals") {


    they("should be able to add rate to the database and find them") {
      val now0 = now
      val rate = Rate(Renting, now0, 88)
      bicycleBusiness.addRate(now0)(rate) should matchPattern {case Right(_) => }

      val now1 = now0.plusSeconds(5)

      bicycleBusiness.findRate(Renting, now1) should be (Some(rate))

      val newRate = Rate(Renting, start=now1.plusSeconds(15), amount=37)

      bicycleBusiness.addRate(now1)(newRate) should matchPattern {case Right(_) => }

      //Getting two rates in the supplied interval
      bicycleBusiness.findRates(Renting, Interval.of(now0, Duration.ofSeconds(100))).toList should be (List(rate, newRate))

      val now2 = newRate.start.plusSeconds(1)
      bicycleBusiness.findRate(Renting, now2) should be (Some(newRate))
    }

    they("should not be able to apply rates retroactively") {
      val startTime = now.minusSeconds(3600)
      val amount: BigDecimal = 100
      bicycleBusiness.addRate(now)(Rate(Renting, startTime, amount)) should matchPattern {case Left(_) => }
      bicycleBusiness.findRate(Renting, now) should be (None)
    }

    they("should only have one one rate active at a time") {
      bicycleBusiness.findRate(Renting, now) should be (None)

      val rentalRate = Rate(Renting, now, 100)
      bicycleBusiness.addRate(now)(rentalRate) should matchPattern {case Right(_) => }
      bicycleBusiness.findRate(Renting, now) should be (Some(rentalRate))

      val newRentalRate = Rate(Renting, now, 105)
      bicycleBusiness.addRate(now)(newRentalRate) should matchPattern {case Right(_) => }
      bicycleBusiness.findRate(Renting, now) should be (Some(newRentalRate))
    }
  }

  describe("InvoiceCalculation") {

    it("should calculate invoice-rows for a rentals") {
      val cyclist = Cyclist(UUID.fromString("d292a7e2-a44e-4eb6-882f-a523e9de3a4f"))

      val rentalRates = List(Rate(Renting, Instant.parse("2019-11-27T19:29:30.127Z"), 20),
        Rate(Renting, Instant.parse("2019-11-27T19:29:33.127Z"), 30),
        Rate(Renting, Instant.parse("2019-11-27T19:29:30.127Z"), 10))

      val serviceRates = List(Rate(Service, Instant.parse("2019-11-27T19:35:30.127Z"), .1),
        Rate(Service, Instant.parse("2019-11-27T19:29:30.127Z"), .2),
        Rate(Service, Instant.parse("2019-11-28T19:29:30.127Z"), .15))

      val rental = BikeRental(cyclist, Instant.parse("2019-11-27T19:29:31.127Z"), Duration.ofMinutes(125))

      val result = BicycleBusiness.calculateInvoiceRows(rentalRates, serviceRates, rental)

      result should have size(2)
    }

  }
  describe("As bookkeepers") {
    val cyclist1 = Cyclist(UUID.randomUUID())
    val cyclist2 = Cyclist(UUID.randomUUID())
    they("should be able to add a charge-sessions, and find them") {
      val session1 = BikeRental(cyclist1, now.minusSeconds(999), Duration.ofSeconds(748))
      bicycleBusiness.addRental(session1) should be (Right())
      val session2 = BikeRental(cyclist2, now.minusSeconds(748), Duration.ofSeconds(748))
      bicycleBusiness.addRental(session2) should be (Right())

      bicycleBusiness.findRentals(Interval.of(now.minusSeconds(999), now)).toSet should be(Set(session1, session2))
    }
    they("should not be able to charge-sessions in the future") {
      val session1 = BikeRental(cyclist1, Instant.now().minusSeconds(747), Duration.ofSeconds(748))
      bicycleBusiness.addRental(session1) should matchPattern {case Left(_) => }
      val session2 = BikeRental(cyclist2, Instant.now().plusSeconds(434), Duration.ofSeconds(748))
      bicycleBusiness.addRental(session2) should matchPattern {case Left(_) => }
    }
    they("should be to get access to invoicing-data") {
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(9000), 100)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(300), 100)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(500), 100)) should matchPattern {case Right(_) => }

      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(2000), 20))  should matchPattern {case Right(_) => }//One session won't have a parking rate
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(350), 30)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(310), 10)) should matchPattern {case Right(_) => }

      bicycleBusiness.addRate(now)(Rate(Service, now.plusSeconds(9000), .1)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Service, now.plusSeconds(450), .2)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Service, now.plusSeconds(100), .15)) should matchPattern {case Right(_) => }

      val now1 = now.plusSeconds(9000)

      bicycleBusiness.addRental(now1)(BikeRental(cyclist1, now.plusSeconds(999), Duration.ofSeconds(748)))
      bicycleBusiness.addRental(now1)(BikeRental(cyclist2, now.plusSeconds(2323), Duration.ofSeconds(38)))

      val interval = Interval.of(now.minusMillis(Duration.ofDays(20).toMillis), Duration.ofDays(30))

      val result = bicycleBusiness.allInvoiceRows(interval)
      result should matchPattern{case Right(_) => }
    }
  }
  describe("As cyclist") {
    val cyclist = Cyclist(UUID.fromString("d292a7e2-a44e-4eb6-882f-a523e9de3a4f"))

    they("should be able to get access to my own report") {
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(9000), .5)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(300), .4)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(500), .22)) should matchPattern {case Right(_) => }

      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(2000), 20))  should matchPattern {case Right(_) => }//One session won't have a parking rate
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(350), 30)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Renting, now.plusSeconds(310), 10)) should matchPattern {case Right(_) => }

      bicycleBusiness.addRate(now)(Rate(Service, now.plusSeconds(2000), .1)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Service, now.plusSeconds(450), .2)) should matchPattern {case Right(_) => }
      bicycleBusiness.addRate(now)(Rate(Service, now.plusSeconds(100), .15)) should matchPattern {case Right(_) => }

      val now1 = now.plusSeconds(9000)

      bicycleBusiness.addRental(now1)(BikeRental(cyclist, now.plusSeconds(400), Duration.ofSeconds(7148)))
      bicycleBusiness.addRental(now1)(BikeRental(cyclist, now.plusSeconds(999), Duration.ofSeconds(748)))
      bicycleBusiness.addRental(now1)(BikeRental(cyclist, now.plusSeconds(2323), Duration.ofSeconds(38)))

      val interval = Interval.of(now.minusMillis(Duration.ofDays(20).toMillis), Duration.ofDays(30))

      val result = reporting.cyclistReport(cyclist, interval)
      println(result.getOrElse(""))
      result should matchPattern{ case Right(_) => }

    }
  }
}
