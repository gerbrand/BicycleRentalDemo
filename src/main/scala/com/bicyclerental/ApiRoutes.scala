package com.bicyclerental

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.bicyclerental.domain._
import org.threeten.extra.Interval

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
                get {
                  parameters(Symbol("rateType").as[RateType], Symbol("date").as[Instant]) { (rateType: RateType, date: Instant) =>
                    complete(domain.findRate(rateType, date))
                  }
                },
                get {
                  parameters(Symbol("rateType").as[RateType], Symbol("interval").as[Interval]) { (rateType: RateType, interval: Interval) =>
                    complete(domain.findRates(rateType, interval))
                  }
                },
                put {
                  entity(as[Rate]) { rate =>
                    completeEither(domain.addRate(Instant.now)(rate).map(ToResponseMarshallable(_)))
                  }
                })
            },
          )
        }
      },
      authenticateBasic(realm = "accountant site", accountantAuthenticator) { _ =>
        concat(pathPrefix("rentals") {
          concat(
            pathEnd {
              concat(
                get {
                  parameters(Symbol("interval").as[Interval]) { (interval: Interval) =>
                    complete(domain.findRentals(interval))
                  }
                },
                put {
                  entity(as[BikeRental]) { session =>
                    completeEither(domain.addRental(Instant.now)(session).map(ToResponseMarshallable(_)))
                  }
                })
            },
          )
        },
          pathPrefix("invoices") {
            get {
              parameters(Symbol("interval").as[Interval]) { (interval: Interval) =>
                complete(domain.allInvoiceRows(interval))
              }
            }
          })
    })
}
