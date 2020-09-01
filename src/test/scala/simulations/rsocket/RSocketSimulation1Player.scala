package simulations.rsocket


import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class RSocketSimulation1Player extends Simulation{
  val csvFeeder = csv("data\\onePlayer.csv")

  val httpConf = http
    .wsBaseUrl("ws://localhost:8080")

  val scn = scenario("RScoket for 1 Player")
    .feed(csvFeeder)
    .exec(ws("Websocket connection").connect("/rsocket"))
    .pause(3)
    // ------------------------------------------------  SUBSCRIPTIONS -----------------------------------------
//    .exec(ws("Subscribe0: ADD/PLAYERS")
//      .sendText("[\"SUBSCRIBE\\nid:sub-0\\ndestination:/pacman/add/players\\n\\n\\u0000\"]")
//    )
//    .exec(ws("Subscribe1: REMOVE/PLAYER")
//      .sendText("[\"SUBSCRIBE\\nid:sub-1\\ndestination:/pacman/remove/player\\n\\n\\u0000\"]")
//    )
//    .exec(ws("Subscribe2: UPDATE/PLAYER")
//      .sendText("[\"SUBSCRIBE\\nid:sub-2\\ndestination:/pacman/update/player\\n\\n\\u0000\"]")
//    )
//    .exec(ws("Subscribe3: UPDATE/MONSTER")
//      .sendText("[\"SUBSCRIBE\\nid:sub-3\\ndestination:/pacman/update/monster\\n\\n\\u0000\"]")
//    )
//    .pause(1)
//    //    // ---------------------------------------   CREATE USERS   ---------------------------------------------------
//    .exec(ws("SEND JOIN/GAME")
//      .sendText("[\"SEND\\ndestination:/app/join/game\\ncontent-length:19\\n\\n" +
//        "{\\\"nickname\\\":\\\"" +
//        "${nickname}" +
//        "\\\"}" +
//        "\\u0000\"]")
//    )
//    .pause(1)
//    .exec(ws("SEND ADD/PLAYER")
//      .sendText("[\"SEND\\ndestination:/app/add/player\\ncontent-length:19\\n\\n" +
//        "{\\\"nickname\\\":\\\"${nickname}\\\"}" +
//        "\\u0000\"]")
//    )
//    .pause(1)
//    // ----------------------------------------------------   MOVEMENTS  ---------------------------------------------
//    .repeat(5, "i") {
//      repeat(24, "j") {
//        exec(
//          session => session.set("x1", session("startPositionX").as[Int] + session("j").as[Int] * 32)
//        ).
//          exec(
//            ws("Send move")
//              .sendText("[\"SEND\\ndestination:/app/send/position\\ncontent-length:55\\n\\n" +
//                "{\\\"nickname\\\":\\\"${nickname}\\\"," + /*19*/
//                "\\\"positionX\\\":\\\"${x1}\\\"," + /*19*/
//                "\\\"positionY\\\":\\\"${startPositionY}\\\"}" + /*18*/
//                "\\u0000\"]").await(200 millis)(
//              ws.checkTextMessage("Sprawdzam pozycje1").check(regex(".*")))
//          ).pause(200 millis)
//      }
//    }
//    .pause(3)
    .exec(ws("Close WS").close)

  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
}
