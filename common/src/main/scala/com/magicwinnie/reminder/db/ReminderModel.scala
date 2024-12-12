package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.mongodb.scala.bson.ObjectId

case class ReminderModel(
  _id: ObjectId = new ObjectId(),
  chatId: Long, // User Telegram ID
  name: String, // Reminder name
  executeAt: DateTime, // Reminder datetime
  repeatIn: Option[Period], // Reminder repeat interval, None in case of a once time one
  timezoneID: String // Timezone Joda ID
)
