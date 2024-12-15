package com.magicwinnie.reminder.state

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.bot4s.telegram.models.{Chat, ChatType, Message}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PerChatStateSpec extends AnyFlatSpec with Matchers {

  "PerChatState" should "store and retrieve chat state" in {
    val state = PerChatState.create[IO, String].unsafeRunSync()

    val chat = Chat(1234L, ChatType.Private)
    implicit val message: Message = Message(1, chat = chat, date = 0)

    state.setChatState("A").unsafeRunSync()

    state
      .withChatState {
        case Some(value) => IO(value shouldBe "A")
        case None        => IO(fail("State not set"))
      }
      .unsafeRunSync()
  }

  it should "clear chat state" in {
    val state = PerChatState.create[IO, String].unsafeRunSync()

    val chat = Chat(5678L, ChatType.Private)
    implicit val message: Message = Message(2, chat = chat, date = 0)

    state.setChatState("B").unsafeRunSync()
    state.clearChatState.unsafeRunSync()

    state
      .withChatState {
        case Some(_) => IO(fail("State was not cleared"))
        case None    => IO.unit
      }
      .unsafeRunSync()
  }

  it should "handle missing state" in {
    val state = PerChatState.create[IO, String].unsafeRunSync()

    val chat = Chat(91011L, ChatType.Private)
    implicit val message: Message = Message(3, chat = chat, date = 0)

    state
      .withChatState {
        case Some(_) => IO(fail("Unexpected state found"))
        case None    => IO.unit
      }
      .unsafeRunSync()
  }

  it should "independent work for different chats" in {
    val state = PerChatState.create[IO, String].unsafeRunSync()

    val chat1 = Chat(1111L, ChatType.Private)
    val chat2 = Chat(2222L, ChatType.Private)

    {
      implicit val message: Message = Message(4, chat = chat1, date = 0)
      state.setChatState("C").unsafeRunSync()
      state
        .withChatState {
          case Some(value) => IO(value shouldBe "C")
          case None        => IO(fail("State not set for chat1"))
        }
        .unsafeRunSync()
    }

    {
      implicit val message: Message = Message(5, chat = chat2, date = 0)
      state.setChatState("D").unsafeRunSync()
      state
        .withChatState {
          case Some(value) => IO(value shouldBe "D")
          case None        => IO(fail("State not set for chat2"))
        }
        .unsafeRunSync()
    }

    {
      implicit val message: Message = Message(4, chat = chat1, date = 0)
      state
        .withChatState {
          case Some(value) => IO(value shouldBe "C")
          case None        => IO(fail("State of chat1 is incorrect"))
        }
        .unsafeRunSync()
    }
  }

  it should "allow updating existing state" in {
    val state = PerChatState.create[IO, String].unsafeRunSync()

    val chat = Chat(3333L, ChatType.Private)
    implicit val message: Message = Message(6, chat = chat, date = 0)

    state.setChatState("E").unsafeRunSync()
    state.setChatState("F").unsafeRunSync()

    state
      .withChatState {
        case Some(value) => IO(value shouldBe "F")
        case None        => IO(fail("State not updated"))
      }
      .unsafeRunSync()
  }

  it should "handle concurrent access safely" in {
    val state = PerChatState.create[IO, String].unsafeRunSync()

    val chat = Chat(4444L, ChatType.Private)
    implicit val message: Message = Message(7, chat = chat, date = 0)

    val operations = List(
      state.setChatState("G"),
      state.setChatState("H"),
      state.clearChatState
    )

    operations.parSequence.unsafeRunSync()

    state
      .withChatState {
        case Some(_) => IO.unit
        case None    => IO.unit
      }
      .unsafeRunSync()
  }
}
