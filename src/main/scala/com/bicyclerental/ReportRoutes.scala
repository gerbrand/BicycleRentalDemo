package com.bicyclerental

import akka.http.scaladsl.server.Directives.{complete, concat, get, parameters, pathEnd, pathPrefix}

import scala.concurrent.ExecutionContext
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bicyclerental.domain._
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation
import io.swagger.annotations.{Api, ApiKeyAuthDefinition, Authorization, BasicAuthDefinition, SecurityDefinition, SwaggerDefinition}
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import org.threeten.extra.Interval

import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, Path, Produces}

class ReportRoutes(reporting: BicycleRentalReporting)(implicit val system: ActorSystem[_], implicit val executionContext: ExecutionContext, timeout: Timeout) {

  import Routes._
  def completeEitherCsv(e: Either[String, String]) = {
    e match {
      case Right(v) =>  complete {
        HttpResponse(entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, v))
      }
      case Left(e) => complete((BadRequest, e))
    }
  }

  val routes: Route = getReports

  @GET
  @Path("/reports")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Authorization("basicAuth")
  @Operation(summary = "Get report on rentals", description = "Retrieve reports on rentals",
    parameters = Array(
      new Parameter(name = "cyclist", in = ParameterIn.QUERY, required=true),
      new Parameter(name = "interval", in = ParameterIn.QUERY, required=true)
    ),
  )
  def getReports: Route = {
    concat(pathPrefix("reports") {
      concat(
        pathEnd {
          concat(
            get {
              parameters(Symbol("cyclist").as[Cyclist], Symbol("interval").as[Interval]) { (cyclist: Cyclist, interval: Interval) => completeEitherCsv(reporting.cyclistReport(cyclist, interval))
              }
            }
          )
        })
    })
  }
}
