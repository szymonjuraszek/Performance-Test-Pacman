package simulations.size

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class WebsocketSimulation100Size extends Simulation {
  val csvFeeder = csv("data/players-10.csv")

  val httpConf = http
    .wsBaseUrl("ws://83.229.84.77:8080")

  val scn = scenario("Websocket for 10 Players")
    .feed(csvFeeder)
    .exec(ws("Websocket connection").connect("/socket"))
    .exec(ws("Connect via STOMP")
      .sendText("CONNECT\naccept-version:1.0,1.1,1.2\nheart-beat:4000,4000\n\n\000")
      .await(2 seconds)(ws.checkTextMessage("check connected message").check(regex(".*CONNECTED.*")))
    )
    .pause(500 millis)
    // ------------------------------------------------  SUBSCRIPTIONS -----------------------------------------
    .exec(ws("Subscribe0: ADD/PLAYERS")
      .sendText("SUBSCRIBE\nid:sub-0\ndestination:/pacman/add/players\n\n\000")
    )
    .exec(ws("Subscribe1: REMOVE/PLAYER")
      .sendText("SUBSCRIBE\nid:sub-1\ndestination:/pacman/remove/player\n\n\000")
    )
    .exec(ws("Subscribe2: UPDATE/PLAYER")
      .sendText("SUBSCRIBE\nid:sub-2\ndestination:/pacman/update/player\n\n\000")
    )
    .exec(ws("Subscribe3: UPDATE/MONSTER")
      .sendText("SUBSCRIBE\nid:sub-3\ndestination:/pacman/update/monster\n\n\000")
    )
    //    .pause(1)
    //    //    // ---------------------------------------   CREATE USERS   ---------------------------------------------------
    .exec(ws("SEND JOIN/GAME")
      .sendText("SEND\ndestination:/app/join/game\ncontent-length:22\n\n" +
        "{\"nickname\":\"${nickname}\"}\000")
    )
    //    .pause(1)
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
              .sendText("SEND\ndestination:/app/send/position\ncontent-length:98\n" +
                "requestTimestamp:${requestTimestamp}" +
                "\n\n" +
                "{\"nickname\":\"${nickname}\"," +
                "\"positionX\":${newX}," +
                "\"positionY\":${y}," +
                "\"score\":0," +
                "\"stepDirection\":\"HOR\"," +
                "\"version\":0}\000"
              )
          )
          .pause(21 millis)
      }
    }
    .pause(2)
    .exec(ws("Close WS").close)

  setUp(scn.inject(
    nothingFor(10 seconds),
    rampUsers(9) during (90 seconds)).protocols(httpConf))
}