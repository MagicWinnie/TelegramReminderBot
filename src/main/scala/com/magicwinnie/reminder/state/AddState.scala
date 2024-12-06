package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.{DateTime, Period}

case class AddState(
  name: Option[String],
  executeAt: Option[DateTime],
  repeatIn: Option[Period]
)

object AddState {
  def empty: AddState = AddState(None, None, None)
}
