package com.magicwinnie.reminder.db

import org.mongodb.scala.bson.ObjectId
import com.github.nscala_time.time.Imports.{DateTime, Period}

case class ReminderModel(
  _id: ObjectId = new ObjectId(),
  chatId: Long,
  name: String,
  executeAt: DateTime,
  repeatIn: Option[Period],
  createdAt: DateTime
)
