package com.bicyclerental

import java.time.{Duration, Instant}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.event.slf4j.SLF4JLogging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bicyclerental.domain._
import com.bicyclerental.persistence.{RateRepository, RentalRepository}

import scala.util.Failure
import scala.util.Success

object BicycleRentalApp extends SLF4JLogging {
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8085)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def insertDefaultRates(business: BicycleBusiness): Unit = {
    log.info("Inserting a few demo-default rates")
    val past = Instant.now.minusMillis(Duration.ofDays(30).toMillis)
    val addToPast: Rate => Either[String, Rate] = business.addRate(past)
    addToPast(Rate(RateType.Renting, past, 20))
    addToPast(Rate(RateType.Service, past, .1))
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>

      implicit val executionContext = context.system.executionContext
      implicit val timeout: Timeout = Timeout.create(context.system.settings.config.getDuration("bicyclerental.routes.ask-timeout"))
      val feeRepository = new RateRepository
      val sessionRepository = new RentalRepository
      val business = new BicycleBusiness(feeRepository, sessionRepository)
      val reporting = new BicycleRentalReporting(business)

      insertDefaultRates(business)


      val apiRoutes =  new ApiRoutes(business)(context.system, executionContext, timeout)
      val reportRoutes =  new ReportRoutes(reporting)(context.system, executionContext, timeout)
      startHttpServer( Route.seal {concat(reportRoutes.routes, apiRoutes.apiRoutes)}, context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "BicycleRental")

  }
}

