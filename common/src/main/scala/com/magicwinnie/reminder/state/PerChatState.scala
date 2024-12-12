package com.magicwinnie.reminder.state

import cats.effect.{Concurrent, Ref}
import cats.syntax.all._
import com.bot4s.telegram.models.Message

/** Manages per-chat state for stateful interactions.
  *
  * Provides methods to set, clear, and retrieve state associated with specific chat contexts.
  *
  * @tparam F
  *   Type of effect context
  * @tparam S
  *   Type of state being managed
  */
trait PerChatState[F[_], S] {

  /** Sets the state for a specific chat.
    *
    * @param value
    *   The state to be set
    * @param msg
    *   The Telegram message providing chat context
    */
  def setChatState(value: S)(implicit msg: Message): F[Unit]

  /** Clears the state for a specific chat.
    *
    * @param msg
    *   The Telegram message providing chat context
    */
  def clearChatState(implicit msg: Message): F[Unit]

  /** Performs an action with the current chat state.
    *
    * @param action
    *   A function that takes the current state and performs an effect
    * @param msg
    *   The Telegram message providing chat context
    */
  def withChatState(action: Option[S] => F[Unit])(implicit msg: Message): F[Unit]
}

object PerChatState {

  /** Creates a new PerChatState instance with concurrent state management.
    *
    * @tparam F
    *   Type of effect context
    * @tparam S
    *   Type of state being managed
    * @return
    *   A PerChatState instance wrapped in an effect
    */
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
