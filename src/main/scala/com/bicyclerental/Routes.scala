package com.bicyclerental

import java.time.Instant
import java.util.UUID

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.util.Timeout
import com.bicyclerental.domain.{Cyclist, RateType}
import org.threeten.extra.Interval

import scala.concurrent.{ExecutionContext, Future}

object Routes {

  def completeEither(e: Either[String, ToResponseMarshallable]) = {
    e match {
      case Right(v) => complete(v)
      case Left(e) => complete((BadRequest, e))
    }
  }

  def toUUID(s: String)(implicit ec: ExecutionContext): Future[UUID] = {
    Future {
      UUID.fromString(s)
    }
  }

  /* Using two user databases instead of roles.
     I'd assume in actual production environment we'd rather want to use some single-sign-on-solution,
     using oauth2 or similar
   */

  val adminUsers = Map("admin" -> "passsword123")

  val accountantUsers = Map("accountant" -> "welkom01")

  def veryBasicAuthenticator(users: Map[String, String])(credentials: Credentials): Option[String] =
    credentials match {
      case p @ Credentials.Provided(id) => users.get(id).filter(p.verify(_))
      case _ => None
    }

  val adminAuthenticator: Credentials => Option[String] = veryBasicAuthenticator(adminUsers)
  val accountantAuthenticator: Credentials => Option[String] = veryBasicAuthenticator(accountantUsers)

  implicit val dateUnmarshaller:FromStringUnmarshaller[Instant] = Unmarshaller.strict((Instant.parse(_)))
  implicit val rateTypeUnmarshaller:FromStringUnmarshaller[RateType] = Unmarshaller.strict(RateType.apply(_))
  implicit val cyclistUnmarshaller:FromStringUnmarshaller[Cyclist] = Unmarshaller.strict(s => Cyclist(UUID.fromString(s)))
  implicit val intervalUnmarshaller:FromStringUnmarshaller[Interval] = Unmarshaller.strict(Interval.parse(_))
}
