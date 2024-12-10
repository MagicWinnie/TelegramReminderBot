package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.mongodb.scala.bson.ObjectId

case class ReminderModel(
  _id: ObjectId = new ObjectId(),
  chatId: Long,
  name: String,
  executeAt: DateTime,
  repeatIn: Option[Period],
  timezoneID: String
)
