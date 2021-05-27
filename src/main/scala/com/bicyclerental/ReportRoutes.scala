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
import org.threeten.extra.Interval

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

  val routes: Route = concat(pathPrefix("reports") {
    concat(
      pathEnd {
        concat(
          get {
            parameters(Symbol("cyclist").as[Cyclist], Symbol("interval").as[Interval]) { (cyclist: Cyclist, interval: Interval) => completeEitherCsv(reporting.cyclistReport(cyclist, interval))
            }
          }
        )
      })})

}
