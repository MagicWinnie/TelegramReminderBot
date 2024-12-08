package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.{DateTime, Period}

sealed trait UserState

object UserState {
  case object AwaitingName extends UserState

  case class AwaitingDate(name: String) extends UserState

  case class AwaitingRepeat(name: String, executeAt: DateTime) extends UserState

  case class Completed(name: String, executeAt: DateTime, repeatIn: Option[Period]) extends UserState
}