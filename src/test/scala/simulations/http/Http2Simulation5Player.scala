package simulations.http

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class Http2Simulation5Player extends Simulation {
  val csvFeeder = csv("data\\fivePlayers.csv")

  val httpProtocol = http
    .enableHttp2
    .baseUrl("https://localhost:8080")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
    .acceptHeader("text/html,application/xhtml+xml,application/json,application/xml;q=0.9,*/*;q=0.8")

  val scn = scenario("myScenario")
    .feed(csvFeeder)
    .exec(
      sse("Get SSE")
        .connect("/emitter/" + "${nickname}")
    )
    .pause(2)
    .exec(
      http("Add player")
        .post("/players")
        .body(StringBody("""{ "nickname": "${nickname}" }""")).asJson
    )
    .pause(1)
    //    // ----------------------------------------------------   MOVEMENTS  ---------------------------------------------
    .repeat(6, "i") {
      repeat(200, "j") {
        exec(
          session => session.set("newX", session("x").as[Int] - session("j").as[Int] * 5).set("timestamp", System.currentTimeMillis().toString())
        ).
          exec(http("Send message")
            .put("/player")
            .header("requestTimestamp", "${timestamp}")
            .body(StringBody("""{ "nickname": "${nickname}", "positionX": ${newX}, "positionY": ${y}, "score": 0, "stepDirection": "HORIZON", "version": 0 } """)).asJson
          ).pause(20 millis)
      }
    }
    .pause(3)
    .exec(
      http("delete player")
        .delete("/emitter/" + "${nickname}"))
    .exec(sse("Close").close())

  setUp(scn.inject(rampUsers(5) during (10 seconds)).protocols(httpProtocol))
}
