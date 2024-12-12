package com.magicwinnie.reminder.state

import cats.effect.{Concurrent, Ref}
import cats.syntax.all._
import com.bot4s.telegram.models.Message

trait PerChatState[F[_], S] {
  def setChatState(value: S)(implicit msg: Message): F[Unit]
  def clearChatState(implicit msg: Message): F[Unit]
  def withChatState(action: Option[S] => F[Unit])(implicit msg: Message): F[Unit]
}

object PerChatState {
  def create[F[_]: Concurrent, S]: F[PerChatState[F, S]] =
    Ref.of[F, Map[Long, S]](Map.empty).map { ref =>
      new PerChatState[F, S] {
        override def setChatState(value: S)(implicit msg: Message): F[Unit] =
          ref.update(_.updated(msg.chat.id, value))

        override def clearChatState(implicit msg: Message): F[Unit] =
          ref.update(_ - msg.chat.id)

        override def withChatState(action: Option[S] => F[Unit])(implicit msg: Message): F[Unit] =
          ref.get.map(_.get(msg.chat.id)).flatMap(action)
      }
    }
}
