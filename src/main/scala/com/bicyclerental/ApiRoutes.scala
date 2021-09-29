package com.bicyclerental

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bicyclerental.domain._
import io.swagger.annotations.Authorization
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.threeten.extra.Interval

import java.time.Instant
import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, PUT, Path, Produces}
import scala.concurrent.ExecutionContext

class ApiRoutes(domain: BicycleBusiness)(implicit val system: ActorSystem[_], implicit val executionContext: ExecutionContext, timeout: Timeout) {
  import Routes._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  val apiRoutes: Route =  concat(
      pathPrefix("rates") {
        authenticateBasic(realm = "admin site", adminAuthenticator) { _ =>
          concat(
            pathEnd {
              concat(
                getRate,
                getRates,
                putRate)
            },
          )
        }
      },
      authenticateBasic(realm = "accountant site", accountantAuthenticator) { _ =>
        concat(pathPrefix("rentals") {
          concat(
            pathEnd {
              concat(
                getRentals,
                putRental)
            },
          )
        },
          getInvoices)
    })

  @PUT
  @Path("/rates")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Authorization("basicAuth")
  @Operation(summary = "Save rate",
    requestBody = new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[Rate]), mediaType = MediaType.APPLICATION_JSON))),
  )
  def putRate = {
    put {
      entity(as[Rate]) { rate =>
        completeEither(domain.addRate(Instant.now)(rate).map(ToResponseMarshallable(_)))
      }
    }
  }

  @GET
  @Path("/rates")
  @Operation(summary = "Query rates",
    parameters = Array(
      new Parameter(name = "rateType", in = ParameterIn.QUERY, required=true),
      new Parameter(name = "interval", in = ParameterIn.QUERY, required=true)
    ),
  )
  def getRates: Route = {
    get {
      parameters(Symbol("rateType").as[RateType], Symbol("interval").as[Interval]) { (rateType: RateType, interval: Interval) =>
        complete(domain.findRates(rateType, interval))
      }
    }
  }

  @GET
  @Path("/rentals")
  @Operation(summary = "Query rentals",
    parameters = Array(
      new Parameter(name = "interval", in = ParameterIn.QUERY, required=true)
    ),
  )
  def getRentals: Route = {
    get {
      parameters(Symbol("interval").as[Interval]) { (interval: Interval) =>
        complete(domain.findRentals(interval))
      }
    }
  }

  @PUT
  @Path("/rentals")
  @Operation(summary = "Save rental",
    requestBody = new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[BikeRental]), mediaType = MediaType.APPLICATION_JSON))),
  )
  def putRental: Route = {
    put {
      entity(as[BikeRental]) { session =>
        completeEither(domain.addRental(Instant.now)(session).map(ToResponseMarshallable(_)))
      }
    }
  }

  @GET
  @Path("/invoices")
  @Operation(summary = "Query invoices",
    parameters = Array(
      new Parameter(name = "interval", in = ParameterIn.QUERY, required=true)
    ),
  )
  def getInvoices: Route = {
    pathPrefix("invoices") {
      get {
        parameters(Symbol("interval").as[Interval]) { (interval: Interval) =>
          complete(domain.allInvoiceRows(interval))
        }
      }
    }
  }

  @GET
  @Path("rates")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Authorization("basicAuth")
  @Operation(summary = "Get rate at certain date",
    parameters = Array(
      new Parameter(name = "rateType", in = ParameterIn.QUERY, required=true),
      new Parameter(name = "date", in = ParameterIn.QUERY, required=true)
    ),
  )
  def getRate = {
    get {
      parameters(Symbol("rateType").as[RateType], Symbol("date").as[Instant]) { (rateType: RateType, date: Instant) =>
        complete(domain.findRate(rateType, date))
      }
    }
  }
}
