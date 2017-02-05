package com.victor.game.war

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, PoisonPill, Props}
import akka.io.Tcp.Received
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import com.victor.game.war.actors.game.GameActor
import com.victor.game.war.message.NumberGenerated
import com.victor.game.war.message.player._
import com.victor.game.war.message.service.{PlayerConnected, WriteToClient}
import org.scalatest._

/**
  * тестирует поведение актора игры
  */
class GameActorTest extends TestKit(ActorSystem("GameActorSystem")) with ImplicitSender with FeatureSpecLike with Matchers with BeforeAndAfterAll{
   {
    scenario("user loose because pressed space before magic number and another won") {
      val parent = TestProbe()
      val firstConnectionMock = TestProbe()
      val secondConnectionMock = TestProbe()
      val firstChildMock = TestProbe()
      val secondChildMock = TestProbe();
      val gameActor = parent.childActorOf(Props(classOf[GameActor],(actorRefFactory: ActorRefFactory,connection: ActorRef,playersNum : Integer)=>{
        if (playersNum % 2 == 0)  firstChildMock.ref else secondChildMock.ref;
      }))
      parent.watch(gameActor)
      parent.send(gameActor,PlayerConnected(firstConnectionMock.ref));
      parent.send(gameActor,PlayerConnected(secondConnectionMock.ref));
      firstChildMock.expectMsg(GameFound)
      secondChildMock.expectMsg(GameFound)
      parent.send(gameActor,NumberGenerated(1))
      firstChildMock.expectMsg(WriteToClient("1\n"))
      secondChildMock.expectMsg(WriteToClient("1\n"))
      firstChildMock.send(gameActor,Received(GameActor.magicInput))
      secondChildMock.send(gameActor,Received(ByteString.fromString("bla bla")))
      firstChildMock.expectMsg(GameTerminated(GameTerminatedNormal(Loose(Faster))))
      secondChildMock.expectMsg(GameTerminated(GameTerminatedNormal(Won(Slower))))
      parent.expectTerminated(gameActor)
    }
    scenario("user won because pressed space first after generating magic number")  {
      val parent = TestProbe()
      val firstConnectionMock = TestProbe()
      val secondConnectionMock = TestProbe()
      val firstChildMock = TestProbe()
      val secondChildMock = TestProbe();
      val gameActor = parent.childActorOf(Props(classOf[GameActor],(actorRefFactory: ActorRefFactory,connection: ActorRef,playersNum : Integer)=>{
        if (playersNum % 2 == 0)  firstChildMock.ref else secondChildMock.ref;
      }))
      parent.watch(gameActor)
      parent.send(gameActor,PlayerConnected(firstConnectionMock.ref));
      parent.send(gameActor,PlayerConnected(secondConnectionMock.ref));
      firstChildMock.expectMsg(GameFound)
      secondChildMock.expectMsg(GameFound)
      parent.send(gameActor,NumberGenerated(1))
      firstChildMock.expectMsg(WriteToClient("1\n"))
      secondChildMock.expectMsg(WriteToClient("1\n"))
      parent.send(gameActor,NumberGenerated(3))
      firstChildMock.expectMsg(WriteToClient("3\n"))
      secondChildMock.expectMsg(WriteToClient("3\n"))
      firstChildMock.send(gameActor,Received(GameActor.magicInput))
      secondChildMock.send(gameActor,Received(ByteString.fromString("bla bla")))
      firstChildMock.expectMsg(GameTerminated(GameTerminatedNormal(Won(Faster))))
      secondChildMock.expectMsg(GameTerminated(GameTerminatedNormal(Loose(Slower))))
      parent.expectTerminated(gameActor)
    }
    scenario("test player disconnected in playing state after magic number generated") {
      val parent = TestProbe()
      val firstConnectionMock = TestProbe()
      val secondConnectionMock = TestProbe()
      val firstChildMock = TestProbe()
      val secondChildMock = TestProbe();
      val gameActor = parent.childActorOf(Props(classOf[GameActor],(actorRefFactory: ActorRefFactory,connection: ActorRef,playersNum : Integer)=>{
        if (playersNum % 2 == 0)  firstChildMock.ref else secondChildMock.ref;
      }))
      parent.watch(gameActor)
      parent.send(gameActor,PlayerConnected(firstConnectionMock.ref));
      parent.send(gameActor,PlayerConnected(secondConnectionMock.ref));
      firstChildMock.expectMsg(GameFound)
      secondChildMock.expectMsg(GameFound)
      parent.send(gameActor,NumberGenerated(1))
      firstChildMock.expectMsg(WriteToClient("1\n"))
      secondChildMock.expectMsg(WriteToClient("1\n"))
      parent.send(gameActor,NumberGenerated(3))
      firstChildMock.expectMsg(WriteToClient("3\n"))
      secondChildMock.expectMsg(WriteToClient("3\n"))
      firstChildMock.ref ! PoisonPill
      secondChildMock.expectMsg(GameTerminated(GameTerminatedUnexpectable))
      parent.expectTerminated(gameActor)
    }
    scenario("test player disconnected in playing state before magic number generated") {
      val parent = TestProbe()
      val firstConnectionMock = TestProbe()
      val secondConnectionMock = TestProbe()
      val firstChildMock = TestProbe()
      val secondChildMock = TestProbe();
      val gameActor = parent.childActorOf(Props(classOf[GameActor],(actorRefFactory: ActorRefFactory,connection: ActorRef,playersNum : Integer)=>{
        if (playersNum % 2 == 0)  firstChildMock.ref else secondChildMock.ref;
      }))
      parent.watch(gameActor)
      parent.send(gameActor,PlayerConnected(firstConnectionMock.ref));
      parent.send(gameActor,PlayerConnected(secondConnectionMock.ref));
      firstChildMock.expectMsg(GameFound)
      secondChildMock.expectMsg(GameFound)
      parent.send(gameActor,NumberGenerated(1))
      firstChildMock.expectMsg(WriteToClient("1\n"))
      secondChildMock.expectMsg(WriteToClient("1\n"))
      firstChildMock.ref ! PoisonPill
      secondChildMock.expectMsg(GameTerminated(GameTerminatedUnexpectable))
      parent.expectTerminated(gameActor)
    }
    scenario("test player disconnected in waiting state")  {
      val parent = TestProbe()
      val firstConnectionMock = TestProbe()
      val firstChildMock = TestProbe()
      val gameActor = parent.childActorOf(Props(classOf[GameActor],(actorRefFactory: ActorRefFactory,connection: ActorRef,playersNum : Integer)=>{
        firstChildMock.ref
      }))
      parent.watch(gameActor)
      parent.send(gameActor,PlayerConnected(firstConnectionMock.ref));
      firstChildMock.ref ! PoisonPill
      parent.expectTerminated(gameActor)
    }


  }
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}