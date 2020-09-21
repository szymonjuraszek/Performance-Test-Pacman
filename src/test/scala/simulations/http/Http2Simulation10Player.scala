package simulations.http

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class Http2Simulation10Player extends Simulation {
  val csvFeeder = csv("data\\tenPlayers.csv")

  val httpProtocol = http
    .enableHttp2
    .baseUrl("https://localhost:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/json,application/xml;q=0.9,*/*;q=0.8")

  val scn = scenario("myScenario")
    .feed(csvFeeder)
    .exec(
      sse("Get SSE")
        .connect("/emitter/" + "${nickname}")
    )
    .pause(1)
    .exec(
      http("Add player")
        .post("/players")
        .body(StringBody("""{ "nickname": "${nickname}" }""")).asJson
    )
    //    // ----------------------------------------------------   MOVEMENTS  ---------------------------------------------
    .repeat(15, "i") {
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

  setUp(scn.inject(rampUsers(10) during (20 seconds)).protocols(httpProtocol))
}
