package com.victor.game.war

/**
  * Created by victor on 02.02.17.
  */
sealed trait GameState
case object Waiting extends GameState
case object RunningMagicNumberNotGenerated extends GameState
case object RunningMagicNumberGenerated extends GameState


