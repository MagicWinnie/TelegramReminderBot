package com.magicwinnie.reminder.state

import cats.effect.Async
import com.bot4s.telegram.models.Message

trait PerChatState[S] {
  private val chatState = collection.mutable.Map[Long, S]()

  def setChatState[F[_]: Async](value: S)(implicit msg: Message): F[Unit] = {
    Async[F].delay {
      chatState.synchronized {
        chatState(msg.chat.id) = value
      }
    }
  }

  def clearChatState(implicit msg: Message): Unit = atomic {
    chatState.remove(msg.chat.id)
    ()
  }

  private def atomic[T](f: => T): T = chatState.synchronized {
    f
  }

  def withChatState[F[_]](f: Option[S] => F[Unit])(implicit msg: Message): F[Unit] = f(getChatState)

  private def getChatState(implicit msg: Message): Option[S] =
    atomic {
      chatState.get(msg.chat.id)
    }
}
