package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.{DateTime, Period}

case class UserState(
  name: Option[String],
  executeAt: Option[DateTime],
  repeatIn: Option[Period]
)

object UserState {
  def empty: UserState = UserState(None, None, None)
}
