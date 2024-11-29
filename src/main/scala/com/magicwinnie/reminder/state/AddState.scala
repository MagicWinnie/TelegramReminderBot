package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.{DateTime, Period}

sealed case class AddState(name: Option[String], executeAt: Option[DateTime], repeatIn: Option[Period])
