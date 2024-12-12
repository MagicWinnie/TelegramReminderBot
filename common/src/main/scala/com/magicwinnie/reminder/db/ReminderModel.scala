package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.mongodb.scala.bson.ObjectId

case class ReminderModel(
  _id: ObjectId = new ObjectId(),
  chatId: Long, // ID Telegram чата пользователя
  name: String, // Название напоминания
  executeAt: DateTime, // Время напоминания
  repeatIn: Option[Period], // Период повторения напоминания, None в случае одноразового напоминания
  timezoneID: String // Joda ID часового пояса
)
