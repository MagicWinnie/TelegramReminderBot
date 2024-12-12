package com.magicwinnie.reminder.db

import cats.effect.{Async, IO}
import cats.syntax.functor._
import com.github.nscala_time.time.Imports.DateTime
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters.{equal, lte}
import org.mongodb.scala.model.Updates.set

/** Интерфейс для работы с напоминаниями в MongoDB.
  *
  * @tparam F
  *   Абстракция эффекта, используемая для работы с асинхронными или эффектно-ориентированными действиями.
  */
trait ReminderRepository[F[_]] {

  /** Создает новое напоминание на основе переданной модели.
    *
    * @param reminder
    *   объект модели ReminderModel, содержащий данные напоминания
    * @return
    *   эффекто-обёртка (F[Unit]), сигнализирующая об успешном завершении операции
    */
  def createReminder(reminder: ReminderModel): F[Unit]

  /** Возвращает список напоминаний для указанного чата.
    *
    * @param chatId
    *   идентификатор чата, для которого необходимо получить напоминания
    * @return
    *   список моделей напоминаний для указанного чата, обернутый в эффект F
    */
  def getRemindersForChat(chatId: Long): F[Seq[ReminderModel]]

  /** Возвращает список напоминаний, которые необходимо выполнить на текущий момент времени.
    *
    * @param currentTime
    *   текущее время, на основе которого определяется, какие напоминания следует выполнить
    * @return
    *   список напоминаний, которые должны быть выполнены
    */
  def getRemindersToExecute(currentTime: DateTime): F[Seq[ReminderModel]]

  /** Обновляет напоминание по представленной модели.
    *
    * @param reminder
    *   объект типа ReminderModel, содержащий данные напоминания, которые нужно обновить
    * @return
    *   эффект F[Unit], представляющий результат выполнения обновления
    */
  def updateReminder(reminder: ReminderModel): F[Unit]

  /** Удаляет напоминание с указанным идентификатором.
    *
    * @param id
    *   идентификатор напоминания, которое требуется удалить
    * @return
    *   результат операции удаления в виде значения типа F[Unit]
    */
  def deleteReminder(id: ObjectId): F[Unit]
}

object ReminderRepository {
  private class LiveReminderRepository[F[_]: Async](collection: MongoCollection[ReminderModel])
    extends ReminderRepository[F] {
    override def createReminder(reminder: ReminderModel): F[Unit] =
      Async[F].fromFuture(Async[F].delay(collection.insertOne(reminder).toFuture())).void

    override def getRemindersForChat(chatId: Long): F[Seq[ReminderModel]] =
      Async[F].fromFuture(Async[F].delay(collection.find(equal("chatId", chatId)).toFuture()))

    override def getRemindersToExecute(currentTime: DateTime): F[Seq[ReminderModel]] =
      Async[F].fromFuture(Async[F].delay(collection.find(lte("executeAt", currentTime)).toFuture()))

    override def updateReminder(reminder: ReminderModel): F[Unit] =
      Async[F]
        .fromFuture(
          Async[F].delay(
            collection.updateOne(equal("_id", reminder._id), set("executeAt", reminder.executeAt)).toFuture()
          )
        )
        .void

    override def deleteReminder(id: ObjectId): F[Unit] =
      Async[F].fromFuture(Async[F].delay(collection.deleteOne(equal("_id", id)).toFuture())).void
  }

  /** Создает экземпляр репозитория напоминаний.
    *
    * @param collection
    *   Коллекция MongoDB, содержащая объекты напоминаний.
    * @return
    *   Инстанс `ReminderRepository` внутри эффекта IO.
    */
  def make[F[_]: Async](collection: MongoCollection[ReminderModel]): IO[ReminderRepository[F]] = {
    IO.pure(new LiveReminderRepository[F](collection))
  }
}
