package com.bicyclerental

import java.time.{Duration, Instant}
import java.util
import java.util.{Comparator, UUID}

import com.bicyclerental.domain.InstantOrdering.reverseOrdering
import io.circe.{Decoder, Encoder}
import org.threeten.extra.Interval

import scala.collection.SortedSet

package object domain {
  sealed trait RateType {
    def validFeeValue(rate: BigDecimal): Boolean = rate >= 0
    def code: String
  }

  object RateType {

    case object Renting extends RateType {
      override val code: String = "R"
    }

    case object Service extends RateType {
      override def validFeeValue(rate: BigDecimal) = rate > 0 && rate <= 0.5

      override val code: String = "S"
    }

    val feeTypes = List(Renting, Service)

    private val feeTypeCodes = feeTypes.map(_.code)
    def apply(code: String): RateType = {
      require(feeTypeCodes.contains(code), s"Code should be one of $feeTypeCodes")
      feeTypes.find(_.code==code).get
    }

    object JsonCodec {
      implicit val decodeFeeType: Decoder[RateType] =
        Decoder.decodeString
          .emap(code =>  feeTypes.find(_.code==code)
                                 .map(Right(_)).getOrElse(Left(s"Code should be one of $feeTypeCodes")))

      implicit val encodeFeeType: Encoder[RateType] =
        Encoder.encodeString.contramap(v => v.code)
    }
  }

  case class Rate(rateType: RateType, start: Instant, amount: BigDecimal ) {
    require(rateType.validFeeValue(amount), s"Amount $amount not valid for $rateType")
  }

  object Rate {
    def valid(rate: Rate): Either[String, Rate] = valid(rate.rateType, rate.start, rate.amount)

    def valid(rateType: RateType, startTime: Instant, amount: BigDecimal): Either[String, Rate] = {
      if (rateType.validFeeValue(amount)) {
        Right(new Rate(rateType = rateType, start = startTime, amount = amount))
      } else
        Left(s"invalid rate ${amount} for ${rateType}")
    }

    /**
     * Find the right rate, assuming rates are sorted in *reverse* ordering
     */
    def find(rates: List[Rate], rateType: RateType, time: Instant) = {
      rates.sortBy(_.start)(reverseOrdering).find(rate => rate.rateType==rateType && ! rate.start.isAfter(time))
    }

    /* Extending Ordered results in a pretty interesting exception:
       https://stackoverflow.com/questions/38334659/diverging-implicit-expansion-for-type-orderingany-with-recursion
     */


  }

  case class Cyclist(id: UUID)
  case class BikeRental(cyclist: Cyclist, start: Instant, duration: Duration ) {
    def interval = Interval.of(start, duration)
  }

  case class InvoiceRow(cyclist: Cyclist, start: Instant, end: Instant, rentalFee: BigDecimal, serviceFee: BigDecimal, totalPrice: BigDecimal, totalServiceFee:BigDecimal)

  case class FeeKey(rateType: RateType, start: Instant) extends Ordered[FeeKey] {
    override def compare(that: FeeKey): Int = (start.toString+rateType.code).compareTo(start.toString+that.rateType.code)
  }

  object FeeKey {
    def apply(rate: Rate) = new FeeKey(rate.rateType, rate.start)
  }

  object InstantOrdering {
    def ordering:Ordering[Instant] = (x: Instant, y: Instant) => x.compareTo(y)
    def reverseOrdering:Ordering[Instant] = (x: Instant, y: Instant) => y.compareTo(x)
  }
}
