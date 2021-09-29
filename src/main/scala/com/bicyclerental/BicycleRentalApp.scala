package com.bicyclerental

import java.time.{Duration, Instant}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.event.slf4j.SLF4JLogging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bicyclerental.domain._
import com.bicyclerental.persistence.{RateRepository, RentalRepository}
import com.bicyclerental.swagger.SwaggerWithUIHttpService
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.security.{SecurityRequirement, SecurityScheme}

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object BicycleRentalApp extends SLF4JLogging {
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Future[Http.ServerBinding] = {
    // Akka HTTP still needs a classic ActorSystem to start
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8085).bindFlow(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
    futureBinding
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
      import akka.pattern.pipe

      implicit val executionContext = context.system.executionContext
      implicit val timeout: Timeout = Timeout.create(context.system.settings.config.getDuration("bicyclerental.routes.ask-timeout"))
      val feeRepository = new RateRepository
      val sessionRepository = new RentalRepository
      val business = new BicycleBusiness(feeRepository, sessionRepository)
      val reporting = new BicycleRentalReporting(business)

      insertDefaultRates(business)


      val apiRoutes =  new ApiRoutes(business)(context.system, executionContext, timeout)
      val reportRoutes =  new ReportRoutes(reporting)(context.system, executionContext, timeout)
      val swaggerUI = new SwaggerWithUIHttpService(){
        override def apiClasses: Set[Class[_]] = Set(classOf[ApiRoutes], classOf[ReportRoutes])
        override val schemes:List[String] = List("http")
        override val host:String = "localhost"

        override val info: Info = Info(version = "0.1", title = "Bicycle Rental App")
        override val security = List(new SecurityRequirement().addList("basicAuth"))

        override val securitySchemes: Map[String, SecurityScheme] = Map("basicAuth" -> new SecurityScheme().`type`(SecurityScheme.Type.HTTP).name("bicycle rental api").scheme("basic"))

      }
      startHttpServer( concat(swaggerUI.routes, Route.seal { pathPrefix("api") { concat(reportRoutes.routes, apiRoutes.apiRoutes)}}), context.system)
      // TODO should we pipe the result?
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "BicycleRental")

  }
}

