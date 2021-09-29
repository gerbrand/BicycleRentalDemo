lazy val akkaHttpVersion = "10.2.6"
lazy val akkaVersion    = "2.6.16"
lazy val circeVersion = "0.14.1"

val jsonDependencies = List(
  "io.circe"                          %% "circe-core"                 % circeVersion,
  "io.circe"                          %% "circe-generic"              % circeVersion,
  "io.circe"                          %% "circe-parser"               % circeVersion,
  "io.circe"                          %% "circe-literal"              % circeVersion,
)

val swaggerDependencies = List(
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.4.2",
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.3.1",
  "com.github.swagger-akka-http" %% "swagger-enumeratum-module" % "2.1.1",
  "io.swagger" % "swagger-annotations" % "1.6.2",
  "org.webjars" % "webjars-locator" % "0.41",
  "org.webjars" % "swagger-ui" % "3.50.0",
)

lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "Gerbrand",
      scalaVersion    := "2.13.5"
    )),
    name := "Bicycle-Rental-Demo",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-actor-typed"         % akkaVersion,
      "de.heikoseeberger"  %% "akka-http-circe"          % "1.29.1",
      "com.typesafe.akka"  %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka"  %% "akka-slf4j"               % akkaVersion,
      "ch.qos.logback"     % "logback-classic"           % "1.2.3",
      "org.threeten"       % "threeten-extra"            % "1.5.0",
      "com.typesafe.akka"  %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka"  %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"      %% "scalatest"                % "3.0.8"         % Test,
      "org.scalacheck"     %% "scalacheck"               % "1.14.2"        % Test
    )
      ++ jsonDependencies
      ++ swaggerDependencies
  )
