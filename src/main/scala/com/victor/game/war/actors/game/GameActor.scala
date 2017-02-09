package com.victor.game.war.actors.game

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorRefFactory, FSM, Terminated}
import akka.io.Tcp._
import akka.util.ByteString
import com.victor.game.war.message.NumberGenerated
import com.victor.game.war.message.player._
import com.victor.game.war.message.service.{PlayerConnected, WriteToClient}
import com.victor.game.war.{GameState, RunningMagicNumberGenerated, RunningMagicNumberNotGenerated, Waiting}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/**
  * константы, необходимые актору игры
  */
object GameActor {
  val MagicNumber                 = 3;
  val MagicInput                  = ByteString(Array[Byte](32.toByte, 13.toByte, 10.toByte));
  val GenerateNumberFrom          = 1;
  val GenerateNumberTo            = 3;
  val GenerateIntervalFromSeconds = 2;
  val GenerateIntervalToSeconds   = 4;
  val PlayersPerGame              = 2;
}

/**
  * актор предславляющий игру, реализован как автомат - fsm
  */
class GameActor(playerConnectionActorFactory: (ActorRefFactory, ActorRef, Integer) => ActorRef)
    extends FSM[GameState, List[ActorRef]] {
  startWith(Waiting, List[ActorRef]()) //изначальное состояние - ожидание
  when(Waiting) {
    case Event(PlayerConnected(connection), stateData) => {
      val handler = playerConnectionActorFactory(context, connection, stateData.length)
      context.watch(handler);
      connection ! Register(handler, useResumeWriting = false);
      val newPlayersList = handler :: stateData;
      if (newPlayersList.length >= GameActor.PlayersPerGame) { //если игроков достаточно начинаем игру
        goto(RunningMagicNumberNotGenerated) using newPlayersList;
      } else {
        stay using newPlayersList;
      }
    }
  }
  when(RunningMagicNumberNotGenerated) {
    case Event(Received(data), stateData) => {
      if (data == GameActor.MagicInput) { //если получили пробел раньше того как сгенерили 3 - игрок нажавший его проиграл
        sender() ! GameTerminated(GameTerminatedNormal(Loose(Faster)));
        stateData.filter(p => p != sender()).foreach(p => p ! GameTerminated(GameTerminatedNormal(Won(Slower))));
        stop();
      } else {
        stay();
      }
    }
    case Event(NumberGenerated(number), stateData) => { //получили от таймера сгенерированное число
      stateData.foreach(p => p ! WriteToClient(number + "\n"));
      if (number == GameActor.MagicNumber) {
        goto(RunningMagicNumberGenerated); //если 3 переходим в новое состояние
      } else { //иначе создаем новый таймер
        setTimer("nextNumberToUser",
                 NumberGenerated(generateNumber()),
                 FiniteDuration(generateTime(), TimeUnit.SECONDS),
                 false);
        stay();
      }
    }

  }
  when(RunningMagicNumberGenerated) {
    case Event(Received(data), stateData) => {
      if (data == GameActor.MagicInput) { //тут наоборот кто нажал первым пробел тот выиграл
        sender() ! GameTerminated(GameTerminatedNormal(Won(Faster)));
        stateData.filter(p => p != sender()).foreach(p => p ! GameTerminated(GameTerminatedNormal(Loose(Slower))))
        stop();
      } else {
        stay();
      }
    }
  }
  whenUnhandled {
    case Event(Terminated(player: ActorRef), stateData) => { //если неожиданно помер один из акторов соединения, значит один из игроков отвалился, шлем сообщения оставшимся и закрываем игру
      if (stateData contains player) {
        stateData.foreach(player => context.unwatch(player)); //чтобы не приходило лишних сообщений тк при остановке игры будут остановлены дочерние акторы - соединения
        stateData.filter(p => (p != sender())).foreach(p => p ! GameTerminated(GameTerminatedUnexpectable));
        log.info("game terminated unexpectable")
        stop();
      } else {
        stay()
      }
    }
    case Event(e, s) => {
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
    }
  }
  onTransition {
    case Waiting -> RunningMagicNumberNotGenerated => { //коллбек при переходе в новое состояние
      nextStateData.foreach(f => f ! GameFound); //шлем всем сообщение
      setTimer("nextNumberToUser",
               NumberGenerated(generateNumber()),
               FiniteDuration(generateTime(), TimeUnit.SECONDS),
               false); //ставим таймер на генерацию числа
    }
  }
  def generateNumber(): Integer = {
    Random.nextInt(GameActor.GenerateNumberTo - GameActor.GenerateNumberFrom + 1) + GameActor.GenerateNumberFrom;
  }
  def generateTime(): Long = {
    Random.nextInt(GameActor.GenerateIntervalToSeconds - GameActor.GenerateIntervalFromSeconds + 1) + GameActor.GenerateIntervalFromSeconds;
  }
}
