package com.bicyclerental.persistence

import java.time._

import akka.event.slf4j.{Logger, SLF4JLogging}
import com.bicyclerental.domain._
import org.threeten.extra.Interval

import scala.collection.{SortedSet, mutable}
import scala.collection.mutable.CollisionProofHashMap

/**
 * The interface the in-memory database
 */
class RateRepository extends SLF4JLogging {
  import InstantOrdering._
  private val rentalRateDb: mutable.CollisionProofHashMap[FeeKey, Rate] = CollisionProofHashMap.empty

  val getRate = rentalRateDb.get(_)

  def addRate(rate: Rate)  = {
    val result = rentalRateDb.put(FeeKey(rate), rate).flatMap(Option(_)) //Needs an ugly flatMap,
                                                                     // because put returns Option(null)
    result.filterNot(_==rate)
      .map(previous => log.info("Replaced {} by {}", previous, rate))
      .getOrElse(log.info("Inserted a rate {}", rate))
  }

  val removeRate = rentalRateDb.remove(_)

  def findRate(rateType: RateType, time: Instant) = {
      Rate.find(rentalRateDb.values.toList, rateType, time)
  }

  /**
   * All rates of supplied type that apply to the supplied interval,
   * which may include the last rate before the start of the interval
   */
  def findFees(rateType: RateType, interval: Interval): List[Rate] = {
    val rates: List[Rate] = rentalRateDb.values.filter(f => f.rateType==rateType).toList.sortBy(_.start)(reverseOrdering)

    val allBeforeEnd = rates.dropWhile(f => f.start.isAfter(interval.getEnd))
    if (allBeforeEnd.isEmpty) {
      List.empty
    } else {
      val firstBeforeStart = allBeforeEnd.find(f => f.start.isBefore(interval.getStart)).getOrElse(allBeforeEnd.last).start
      val intervalThatIncludesLatestFeeBefore = Interval.of(firstBeforeStart, interval.getEnd)
      allBeforeEnd
        .filter(f =>  intervalThatIncludesLatestFeeBefore.contains(f.start))
        .sortBy(f => f.start)(ordering)
    }


  }

  def allRates() = rentalRateDb.values

  def clear() = rentalRateDb.clear

}





