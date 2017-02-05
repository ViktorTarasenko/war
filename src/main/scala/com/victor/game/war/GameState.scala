package com.victor.game.war

/**
  * состояние игры
  */
sealed trait GameState
case object Waiting extends GameState
case object RunningMagicNumberNotGenerated extends GameState
case object RunningMagicNumberGenerated extends GameState


