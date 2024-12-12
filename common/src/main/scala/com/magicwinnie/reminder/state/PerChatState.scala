package com.magicwinnie.reminder.state

import cats.effect.{Concurrent, Ref}
import cats.syntax.all._
import com.bot4s.telegram.models.Message

/** Трейт для управления состоянием чата.
  *
  * @tparam F
  *   эффект, используемый для асинхронного или другого управления потоками данных.
  * @tparam S
  *   тип данных, представляющий состояние чата.
  */
trait PerChatState[F[_], S] {

  /** Устанавливает новое состояние для текущего чата.
    *
    * @param value
    *   новое состояние чата, которое нужно установить.
    * @param msg
    *   объект сообщения, содержащий контекст выполнения.
    * @return
    *   возвращает эффект типа F[Unit], сигнализирующий об успешном завершении операции.
    */
  def setChatState(value: S)(implicit msg: Message): F[Unit]

  /** Сбрасывает состояние чата, связанного с переданным сообщением.
    *
    * @param msg
    *   сообщение, на основе которого идентифицируется чат
    * @return
    *   эффект, представляющий выполнение операции очистки состояния чата
    */
  def clearChatState(implicit msg: Message): F[Unit]

  /** Метод применяется для работы с состоянием чата.
    *
    * @param action
    *   функция, принимающая текущее состояние чата (опционально) и возвращающая эффект типа F[Unit].
    * @param msg
    *   текущее сообщение, которое необходимо для выполнения действия.
    * @return
    *   результат выполнения эффекта F[Unit].
    */
  def withChatState(action: Option[S] => F[Unit])(implicit msg: Message): F[Unit]
}

object PerChatState {

  /** Создает экземпляр `PerChatState[F, S]` для управления состоянием чата.
    *
    * @return
    *   Функциональный эффект, возвращающий экземпляр `PerChatState[F, S]`, который реализует методы для задания,
    *   очистки и работы с состоянием чата.
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
