package com.victor.game.war

import akka.actor.{ActorSystem, Props}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import com.victor.game.war.actors.network.PlayerConnectionActor
import com.victor.game.war.message.player._
import com.victor.game.war.message.service.WriteToClient
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


/**
  * тестирует поведеление актора отвечающего за сетевое соединение с юзером
  */
class PlayerConnectionActorTest extends TestKit(ActorSystem("GameActorSystem")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll{
  "Player connection actor" must {

    "say message to user" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
    }
    "forward 'Received' message to its parent" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.send(playerConnectionActor,Received(ByteString("test")));
      parent.expectMsg(Received(ByteString("test")))
    }
    "write to connection if WriteToClient Sent" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,WriteToClient("test"));
      connectionMock.expectMsg(Write(ByteString("test")));
    }
    "send message to user when game found" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,GameFound);
      connectionMock.expectMsg(Write(ByteString("Противник найден. Нажмите пробел, когда увидите цифру 3\n")));
    }
    "terminate on peer closed" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.watch(playerConnectionActor);
      connectionMock.send(playerConnectionActor,PeerClosed);
      parent.expectTerminated(playerConnectionActor);
    }
    "say to user on his victory faster and terminate" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.watch(playerConnectionActor)
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,GameTerminated(GameTerminatedNormal(Won(Faster))))
      connectionMock.expectMsg(Write(ByteString("Вы нажали пробел первым и победили!\n")))
      parent.expectTerminated(playerConnectionActor);
    }
    "say to user on his victory slower and terminate" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.watch(playerConnectionActor)
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,GameTerminated(GameTerminatedNormal(Won(Slower))));
      connectionMock.expectMsg(Write(ByteString("Ваш противник поспешил и вы выйграли!\n")));
      parent.expectTerminated(playerConnectionActor);
    }
    "say to user on his loose faster and terminate" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.watch(playerConnectionActor)
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,GameTerminated(GameTerminatedNormal(Loose(Faster))));
      connectionMock.expectMsg(Write(ByteString("Вы поспешили и проиграли\n")));
      parent.expectTerminated(playerConnectionActor);
    }
    "say to user on his loose slower and terminate" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.watch(playerConnectionActor)
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,GameTerminated(GameTerminatedNormal(Loose(Slower))));
      connectionMock.expectMsg(Write(ByteString("Вы не успели и проиграли!\n")));
      parent.expectTerminated(playerConnectionActor);
    }
    "say to user on game finished unexpectable and terminate" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      parent.watch(playerConnectionActor)
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,GameTerminated(GameTerminatedUnexpectable))
      connectionMock.expectMsg(Write(ByteString("Игра закончена потому что другой игрок покинул нас!\n")))
      parent.expectTerminated(playerConnectionActor);
    }
    "do nothing on other messages" in {
      val parent = TestProbe()
      val connectionMock = TestProbe()
      val playerConnectionActor = parent.childActorOf(Props[PlayerConnectionActor](new PlayerConnectionActor(connectionMock.ref)))
      connectionMock.expectMsg(Write(ByteString("Привет! Попробую найти тебе противника!\n")))
      parent.send(playerConnectionActor,"test")
      connectionMock.expectNoMsg()
    }

  }
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}
