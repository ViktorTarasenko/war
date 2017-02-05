package com.victor.game.war.actors.network

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import com.victor.game.war.message.player.{Faster, _}
import com.victor.game.war.message.service.WriteToClient

/**
  * актор который отвечает за соединение игрока и подписан на его сокет, создается от аргумента connection - актор соединения,
  * передается при создании
  */
class PlayerConnectionActor(connection : ActorRef) extends Actor{
  val log = Logging(context.system, this);;
  context watch connection;
  connection ! Write(ByteString("Привет! Попробую найти тебе противника!\n"));
  override def receive: Receive = {
    case r @ Received(data) => {
      context.parent ! r;
    }
    case PeerClosed => {
      context stop self;
    }
    case GameTerminated(reason) => {
      val message = getEndGameReason(reason);
      connection ! Write(ByteString(message.toString))
      context stop self
    }
    case GameFound => {
      connection ! Write(ByteString("Противник найден. Нажмите пробел, когда увидите цифру 3\n"))
    }
    case WriteToClient(message) => {
      connection ! Write(ByteString(message.toString));
    }
    case _ => {
      log.info("got unknown message");
    }

  }
  def getEndGameReason(reason: GameTerminatedReason): String ={
    reason match {
      case GameTerminatedUnexpectable=> {
       "Игра закончена потому что другой игрок покинул нас!\n"
      }
      case GameTerminatedNormal(victory) => {
        victory match {
          case Won(reason)=>{
            reason match {
              case Faster=>{
               "Вы нажали пробел первым и победили!\n"
              }
              case Slower=>{
                "Ваш противник поспешил и вы выйграли!\n"
              }
            }
          }
          case Loose(reason)=>{
            reason match {
              case Faster=>{
                "Вы поспешили и проиграли\n";
              }
              case Slower=>{
                "Вы не успели и проиграли!\n"
              }
            }
          }
        }
      }
    }
  }
}

