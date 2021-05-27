package com.bicyclerental.persistence

import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

import akka.event.slf4j.SLF4JLogging
import com.bicyclerental.domain._
import org.threeten.extra.Interval

import scala.jdk.CollectionConverters
import scala.jdk.StreamConverters
import scala.collection.mutable.{ArrayBuffer, TreeSet}

/**
 * The interface the in-memory database for bike-rentals
 */
class RentalRepository extends SLF4JLogging {
  private val bikeRentals: ArrayBuffer[BikeRental] = new ArrayBuffer[BikeRental]

  def add(bikeRental: BikeRental):Unit  = {
    bikeRentals.addOne(bikeRental)
  }

  /**
   * Returns all rentals that start (but to do necessarily end) in the supplied interval
   */
  def findByInterval(interval: Interval): Iterable[BikeRental] = {
    bikeRentals.filter(s => interval.isConnected(s.interval))
  }

  /**
   * Returns all rentals of supplied cyclist, matching the interval as described in findByInterval
   *
   */
  def find(cyclist: Cyclist, interval: Interval): Iterable[BikeRental] = {
    findByInterval(interval).filter(s => s.cyclist==cyclist)
  }


  def clear() = bikeRentals.clear

}



