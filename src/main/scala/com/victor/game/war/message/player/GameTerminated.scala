package com.victor.game.war.message.player

sealed trait GameResultReason
case object Faster extends GameResultReason
case object Slower extends GameResultReason

sealed trait Victory
case class Loose(reason: GameResultReason) extends Victory
case class Won(reason: GameResultReason) extends Victory

trait GameTerminatedReason
case class GameTerminatedNormal(victory: Victory) extends GameTerminatedReason
case object GameTerminatedUnexpectable extends GameTerminatedReason

case class GameTerminated(gameTerminatedReason: GameTerminatedReason)


