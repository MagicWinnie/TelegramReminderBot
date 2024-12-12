package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.DateTime

sealed trait UserState

object UserState {
  case object AwaitingName extends UserState

  case class AwaitingDate(name: String) extends UserState

  case class AwaitingRepeat(name: String, executeAt: DateTime) extends UserState
}
