package com.magicwinnie.reminder.db

import cats.effect.{Async, IO}
import cats.syntax.functor._
import com.github.nscala_time.time.Imports.DateTime
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters.{equal, lte}
import org.mongodb.scala.model.Updates.set

trait ReminderRepository[F[_]] {
  def createReminder(reminder: ReminderModel): F[Unit]
  def getRemindersForChat(chatId: Long): F[Seq[ReminderModel]]
  def getRemindersToExecute(currentTime: DateTime): F[Seq[ReminderModel]]
  def updateReminder(reminder: ReminderModel): F[Unit]
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

  def make[F[_]: Async](collection: MongoCollection[ReminderModel]): IO[ReminderRepository[F]] = {
    IO.pure(new LiveReminderRepository[F](collection))
  }
}
