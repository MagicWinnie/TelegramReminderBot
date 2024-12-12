package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.DateTime

sealed trait UserState

/** Объект UserState определяет состояния пользователя в системе, а также связанные с ними данные, до записи их в базу
  * данных.
  */
object UserState {

  /** Представляет собой состояние пользователя, в котором мы находимся в ожидании от него названия напоминания.
    */
  case object AwaitingName extends UserState

  /** Представляет собой состояние пользователя, в котором мы находимся в ожидании от него времени напоминания.
    *
    * @param name
    *   Уже полученное название напоминания
    */
  case class AwaitingDate(name: String) extends UserState

  /** Представляет собой состояние пользователя, в котором мы находимся в ожидании от него количества дней, через
    * которое нужно повторить напоминание.
    *
    * @param name
    *   Уже полученное название напоминания
    * @param executeAt
    *   Уже полученный объект DateTime, указывающий время напоминания.
    */
  case class AwaitingRepeat(name: String, executeAt: DateTime) extends UserState
}
