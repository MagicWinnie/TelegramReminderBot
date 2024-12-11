package com.magicwinnie.reminder.state

import com.github.nscala_time.time.Imports.DateTimeZone
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserStateSpec extends AnyFlatSpec with Matchers {

  "UserState" should "correctly create AwaitingName state" in {
    val state: UserState = UserState.AwaitingName
    state shouldBe UserState.AwaitingName
  }

  it should "correctly create AwaitingDate state" in {
    val name = "Event"
    val state: UserState = UserState.AwaitingDate(name)

    state match {
      case UserState.AwaitingDate(n) => n shouldBe name
      case _                         => fail("State is not AwaitingDate")
    }
  }

  it should "correctly create AwaitingRepeat state" in {
    val name = "Event"
    val date = DateTime.now(DateTimeZone.UTC)
    val state: UserState = UserState.AwaitingRepeat(name, date)

    state match {
      case UserState.AwaitingRepeat(n, d) =>
        n shouldBe name
        d shouldBe date
      case _ => fail("State is not AwaitingRepeat")
    }
  }

  it should "work with empty event name for AwaitingDate state" in {
    val emptyName = ""
    val state: UserState = UserState.AwaitingDate(emptyName)

    state match {
      case UserState.AwaitingDate(n) => n shouldBe emptyName
      case _                         => fail("State is not AwaitingDate")
    }
  }
}
