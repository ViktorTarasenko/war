package com.victor.game.war
import akka.actor.{ActorSystem, Props}
import com.victor.game.war.actors.game.GamesManagerActor

object EntryPoint extends App {
  val PortNameOption    = "--port"
  val DefaultPort       = 23
  val networkActorProps = Props(classOf[GamesManagerActor], getPort())
  val system            = ActorSystem("GameActorSystem")
  val myActor           = system.actorOf(networkActorProps, "networkActor")
  def getPort(): Integer = {
    if ((args.length >= 2) && (args(0) == PortNameOption))
      args(1).toInt
    else DefaultPort
  }
}
