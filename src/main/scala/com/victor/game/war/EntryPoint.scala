package com.victor.game.war
import akka.actor
import akka.actor.{ActorSystem, Props}
import akka.io
import com.victor.game.war.actors.game.GamesManagerActor

object EntryPoint extends App {
  val networkActorProps = Props[GamesManagerActor];
  val system = ActorSystem("networkSystem");
  val myActor = system.actorOf(networkActorProps, "networkActor");
}


