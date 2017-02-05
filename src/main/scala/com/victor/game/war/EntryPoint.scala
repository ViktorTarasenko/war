package com.victor.game.war
import akka.actor.{ActorSystem, Props}
import com.victor.game.war.actors.game.GamesManagerActor

object EntryPoint extends App {
  val networkActorProps = Props[GamesManagerActor];
  val system = ActorSystem("GameActorSystem");
  val myActor = system.actorOf(networkActorProps, "networkActor");
}


