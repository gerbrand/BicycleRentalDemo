* Basic authentication is used with a fixed in-memory user database. See Routes.scala
  Should use something using OAuth2. Might extend the example to use keycloak.
* Should add something to create/setup a docker image.  
* I'm using```require``` to enforce certain rules in case class constructor.
  Require throws an exception, which is [not very fp-like](https://codereview.stackexchange.com/questions/60645/reducing-boilerplate-when-validating-parameters-and-using-a-tuple-for-the-parame)
  Maybe I have to consider something like Validated
* No use of Future, Stream, Task or similar is used yet. Should I use scala's Future, Cats Effect, or something of ZIO? What's cool and what's cancelled?
* Should implement persistency, e.g. storing in a database properly. Maybe Slick or Doobie ?
* Maybe add something to stream the csv. Kind of cool and not to hard with libraries that support streaming like Slick along with Akka Streams.
