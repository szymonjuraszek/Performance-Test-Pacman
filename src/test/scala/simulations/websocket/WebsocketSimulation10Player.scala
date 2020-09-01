package simulations.websocket

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class WebsocketSimulation10Player extends Simulation {
  val csvFeeder = csv("data\\tenPlayers.csv")

  val httpConf = http
    .wsBaseUrl("ws://localhost:8080")

  val scn = scenario("Websocket for 10 Players")
    .feed(csvFeeder)
    .exec(ws("Websocket connection").connect("/socket"))
    .exec(ws("Connect via STOMP")
      .sendText("CONNECT\naccept-version:1.0,1.1,1.2\nheart-beat:4000,4000\n\n\000")
      .await(2 seconds)(ws.checkTextMessage("check connected message").check(regex(".*CONNECTED.*")))
    )
    .pause(2)
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
    .pause(1)
    //    //    // ---------------------------------------   CREATE USERS   ---------------------------------------------------
    .exec(ws("SEND JOIN/GAME")
      .sendText("SEND\ndestination:/app/join/game\ncontent-length:23\n\n" +
        "{\"nickname\":\"${nickname}\"}\000")
    )
    .pause(1)
    .exec(ws("SEND ADD/PLAYER")
      .sendText("SEND\ndestination:/app/add/player\ncontent-length:23\n\n" +
        "{\"nickname\":\"${nickname}\"}\000")
    )
    //    //    // ---------------------------------------   MOVEMENTS   ---------------------------------------------------
    .repeat(5, "i") {
      repeat(200, "j") {
        exec(
          session => session
            .set("newX", session("x").as[Int] - session("j").as[Int] * 4)
            .set("requestTimestamp", System.currentTimeMillis())
        )
          .exec(
            ws("Send move")
              .sendText("SEND\ndestination:/app/send/position\ncontent-length:103\n" +
                "requestTimestamp:${requestTimestamp}" +
                "\n\n" +
                "{\"nickname\":\"${nickname}\"," +
                "\"positionX\":${newX}," +
                "\"positionY\":${y}," +
                "\"score\":0," +
                "\"stepDirection\":\"HORIZON\"," +
                "\"version\":0}\000"
              ).await(20 millis)(
              ws.checkTextMessage("Check answer").check(regex(".*")))
          )
          .pause(20 millis)
      }
    }
    .pause(10)
    .exec(ws("Close WS").close)

  setUp(scn.inject(rampUsers(10) during (10 seconds)).protocols(httpConf))
}
