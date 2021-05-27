# Introduction

This a demo application demonstrating Akka, AkkaHttp and Circe, using Scala.

The application Bicycle-Rental provides basic functionality for the Bicycle backend:

* Submitting and querying rental rates via JSON. See ```src/test/scripts``` for examples using curl.
* Rates are stored in an in-memory database
* A few demo rates are inserted on start-up. See BicycleRentalApp
# Installing and running
* Application is written in Scala 2.13
* Requires Java 11 or later and sbt to build.
* Run using ```sbt run```
* Run tests ```sbt test```

# Technical design and remarks
* Akka http is used
* Basic authentication is used with a fixed in-memory user database. See Routes.scala
* Main class BicycleRentalApp should be run to start the application
* No use of Future, Stream, Task or similar is used yet.
* In persistence contains repository classes to access the fake inmemory database. My next sideprojecdt is to add slick as well
* As javax.time  [omits Interval](https://stackoverflow.com/questions/22150722/is-there-a-class-in-java-time-comparable-to-the-joda-time-interval), so I added [ThreeTen-Extra](https://github.com/ThreeTen/threeten-extra).
 
