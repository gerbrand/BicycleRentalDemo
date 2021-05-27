lazy val akkaHttpVersion = "10.1.10"
lazy val akkaVersion    = "2.6.0"
lazy val circeVersion = "0.12.3"

val jsonDependencies: List[ModuleID] = List(
  "io.circe"                          %% "circe-core"                 % circeVersion,
  "io.circe"                          %% "circe-generic"              % circeVersion,
  "io.circe"                          %% "circe-parser"               % circeVersion,
  "io.circe"                          %% "circe-literal"              % circeVersion,
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
    ) ++ jsonDependencies
  )
