package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.DateTime

/** Represents the different states of user interaction during reminder creation process. */
sealed trait UserState

object UserState {

  /** Initial state when user starts creating a reminder. Waiting for the reminder name to be entered.
    */
  case object AwaitingName extends UserState

  /** State after name is provided, waiting for the reminder date.
    *
    * @param name
    *   The name of the reminder previously entered
    */
  case class AwaitingDate(name: String) extends UserState

  /** State after date is set, waiting for repeat interval configuration.
    *
    * @param name
    *   The name of the reminder
    * @param executeAt
    *   The scheduled date and time for the reminder
    */
  case class AwaitingRepeat(name: String, executeAt: DateTime) extends UserState
}
