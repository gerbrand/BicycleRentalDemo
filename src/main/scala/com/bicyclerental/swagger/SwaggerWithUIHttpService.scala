package com.bicyclerental.swagger

import akka.http.scaladsl.server.Route
import com.bicyclerental.{ApiRoutes, ReportRoutes}
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.security.{SecurityRequirement, SecurityScheme}
import org.webjars.WebJarAssetLocator

import scala.util.{Failure, Success, Try}

/**
 * Swagger with the [[https://swagger.io/tools/swagger-ui swaggerUI]].
 */
abstract class SwaggerWithUIHttpService() extends SwaggerHttpService {
  //From https://github.com/ThoughtWorksInc/akka-http-webjars/blob/master/src/main/scala/com/thoughtworks/akka/http/WebJarsSupport.scala
  val webJarAssetLocator = new WebJarAssetLocator()

  def webJars(webJarName: String): Route = {
    extractUnmatchedPath { path =>
      Try(webJarAssetLocator.getFullPath(webJarName, path.toString)) match {
        case Success(fullPath) =>
          getFromResource(fullPath)
        case Failure(_: IllegalArgumentException) =>
          reject
        case Failure(e) =>
          failWith(e)
      }
    }
  }

  val swaggerUiRoute = {
    // Please not the api-docs has to match the api-docs in the html, not yet turned into a template
     pathPrefix("api-docs") {  pathEndOrSingleSlash {getFromResource("html/swagger-index.html")} ~  webJars("swagger-ui") }
  }

  override val routes = super.routes ~ swaggerUiRoute
 }
