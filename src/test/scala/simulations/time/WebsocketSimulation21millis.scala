package simulations.time

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class WebsocketSimulation21millis extends Simulation {
  val csvFeeder = csv("data/players-10.csv")

  val httpConf = http
//    .wsBaseUrl("ws://83.229.84.77:8080")
    .wsBaseUrl("ws://localhost:8080")

  val scn = scenario("Websocket + STOMP + Json for 10 Players")
    .feed(csvFeeder)
    .exec(ws("Websocket connection").connect("/socket"))
    .exec(ws("Connect via STOMP")
      .sendText("CONNECT\naccept-version:1.0,1.1,1.2\nheart-beat:4000,4000\n\n\000")
    )
    .pause(200 millis)
    // ------------------------------------------------  SUBSCRIPTIONS -----------------------------------------
    .exec(ws("Subscribe2: UPDATE/PLAYER")
      .sendText("SUBSCRIBE\nid:sub-2\ndestination:/pacman/update/player\n\n\000")
    )
    .exec(ws("Subscribe3: UPDATE/MONSTER")
      .sendText("SUBSCRIBE\nid:sub-3\ndestination:/pacman/update/monster\n\n\000")
    )
    //    //    // ---------------------------------------   CREATE USERS   ---------------------------------------------------
    .exec(ws("SEND JOIN/GAME")
      .sendText("SEND\ndestination:/app/join/game\ncontent-length:22\n\n" +
        "{\"nickname\":\"${nickname}\"}\000")
    )
    .exec(ws("SEND ADD/PLAYER")
      .sendText("SEND\ndestination:/app/add/player\ncontent-length:22\n\n" +
        "{\"nickname\":\"${nickname}\"}\000")
    )
    //    //    // ---------------------------------------   MOVEMENTS   ---------------------------------------------------
    .repeat(100, "i") {
      repeat(150, "j") {
        exec(
          session => session
            .set("newX", session("x").as[Int] - session("j").as[Int] * 4)
            .set("requestTimestamp", System.currentTimeMillis())
        )
          .exec(
            ws("Send move")
              .sendText("SEND\ndestination:/app/send/position\ncontent-length:120\n" +
                "requestTimestamp:${requestTimestamp}" +
                "\n\n" +
                "{\"nickname\":\"${nickname}\"," +
                "\"positionX\":${newX}," +
                "\"positionY\":${y}," +
                "\"score\":0," +
                "\"stepDirection\":\"HOR\"," +
                "\"version\":0," +
                "\"additionalData\":[]}\000"
              )
          )
          .pause(21 millis)
      }
    }
    .pause(2)
    .exec(ws("Close WS").close)

  setUp(scn.inject(
    nothingFor(10 seconds),
    rampUsers(10) during (100 seconds)).protocols(httpConf))
}
