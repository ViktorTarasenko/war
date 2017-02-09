package com.victor.game.war.actors.game

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props, Terminated}
import akka.event.Logging
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import com.victor.game.war.actors.network.PlayerConnectionActor
import com.victor.game.war.message.service.PlayerConnected

/**
  * корневой актор, который принимает соединения, создает инстансы игр
  */
class GamesManagerActor(port: Integer) extends Actor {
  import context.system
  IO(Tcp) ! (Bind(self, new InetSocketAddress("localhost", port)))
  val log       = Logging(context.system, this)
  val processor = replaceGame()
  override def receive = {
    case anyMessage @ _ => {
      processor(anyMessage)
    }
  }
  def process(message: Any, waitingPlayersNum: Integer, lookupGame: () => ActorRef): Unit = {
    message match {
      case Bound(localAddress) => {
        log.info("bound")
      }
      case CommandFailed(_: Bind) => {
        log.info("stopped")
        context.stop(self)
      }
      case message @ Connected(remote, local) => {
        log.info("connected")
        val game       = lookupGame()
        val connection = sender()
        game ! PlayerConnected(connection)
        val newProcessFunction = getNewProcessFunction(lookupGame, waitingPlayersNum + 1, message)
        val toBecome: Receive = {
          case anyMessage @ _ => {
            newProcessFunction(anyMessage)
          }
        }
        context.become(toBecome)
      }
      case _ => {
        val newProcessFunction = getNewProcessFunction(lookupGame, waitingPlayersNum, message)
        val toBecome: Receive = {
          case anyMessage @ _ => {
            newProcessFunction(anyMessage)
          }
        }
        context.become(toBecome)
      }
    }
  }
  //lookup game - фабричная ф-я для получения игры, в которую добавляем игроков
  def getNewProcessFunction(lookupGame: () => ActorRef, waitingPlayersNum: Integer, message: Any): (Any) => Unit = {
    message match {
      case Terminated(game: ActorRef) => { //отвалилась ждущая игра
        if (game == lookupGame()) {
          replaceGame(lookupGame)
        } else {
          process(_: Any, waitingPlayersNum, lookupGame)
        }
      }
      case Connected(remote, local) => {
        if ((waitingPlayersNum >= GameActor.PlayersPerGame)) {
          replaceGame(lookupGame) //если достаточно игроков, то в новой функции обработки заменяем инстанс игры
        } else {
          process(_: Any, waitingPlayersNum, lookupGame)
        }
      }
      case _ => {
        process(_: Any, waitingPlayersNum, lookupGame)
      }

    }
  }
  def replaceGame(lookupGame: () => ActorRef = () => ActorRef.noSender): (Any) => Unit = {
    if (lookupGame() != ActorRef.noSender) {
      context unwatch lookupGame()
    }
    val factory = (actorRefFactory: ActorRefFactory, connection: ActorRef, playersNum: Integer) => {
      actorRefFactory.actorOf(Props(classOf[PlayerConnectionActor], connection))
    }
    val newGame = context.actorOf(Props(classOf[GameActor], factory))
    context watch newGame
    process(_: Any, 0, () => newGame)
  }

}
