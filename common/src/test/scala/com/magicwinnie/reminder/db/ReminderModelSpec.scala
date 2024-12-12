package com.magicwinnie.reminder.db

import com.github.nscala_time.time.Imports.{DateTime, Period}
import org.joda.time.DateTimeZone
import org.mongodb.scala.bson.ObjectId
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReminderModelSpec extends AnyFlatSpec with Matchers {
  val chatId = 123456789L
  val name = "A"
  val executeAt: DateTime = DateTime.now(DateTimeZone.UTC)
  val timezoneID = "UTC"

  "ReminderModel" should "be created successfully" in {
    val id = new ObjectId()
    val repeatIn = Some(Period.days(1))

    val reminder = ReminderModel(
      _id = id,
      chatId = chatId,
      name = name,
      executeAt = executeAt,
      repeatIn = repeatIn,
      timezoneID = timezoneID
    )

    reminder._id shouldBe id
    reminder.chatId shouldBe chatId
    reminder.name shouldBe name
    reminder.executeAt shouldBe executeAt
    reminder.repeatIn shouldBe repeatIn
    reminder.timezoneID shouldBe timezoneID
  }

  it should "be created successfully without passing ObjectID" in {
    val repeatIn = Some(Period.days(1))

    val reminder = ReminderModel(
      chatId = chatId,
      name = name,
      executeAt = executeAt,
      repeatIn = repeatIn,
      timezoneID = timezoneID
    )

    reminder.chatId shouldBe chatId
    reminder.name shouldBe name
    reminder.executeAt shouldBe executeAt
    reminder.repeatIn shouldBe repeatIn
    reminder.timezoneID shouldBe timezoneID
  }

  it should "be created successfully without repeat" in {
    val reminder = ReminderModel(
      chatId = chatId,
      name = name,
      executeAt = executeAt,
      repeatIn = None,
      timezoneID = timezoneID
    )

    reminder.chatId shouldBe chatId
    reminder.name shouldBe name
    reminder.executeAt shouldBe executeAt
    reminder.repeatIn shouldBe None
    reminder.timezoneID shouldBe timezoneID
  }

}
