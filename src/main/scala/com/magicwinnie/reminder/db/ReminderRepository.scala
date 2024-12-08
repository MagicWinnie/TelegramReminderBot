package com.magicwinnie.reminder.db

import cats.effect.Async
import cats.syntax.functor._
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters.equal

class ReminderRepository[F[_]: Async](collection: MongoCollection[ReminderModel]) {
  def createReminder(reminder: ReminderModel): F[Unit] = {
    Async[F].fromFuture(Async[F].delay(collection.insertOne(reminder).toFuture())).void
  }

  def getRemindersForChat(chatId: Long): F[Seq[ReminderModel]] = {
    Async[F].fromFuture(Async[F].delay(collection.find(equal("chatId", chatId)).toFuture()))
  }

  def deleteReminder(id: ObjectId): F[Unit] = {
    Async[F].fromFuture(Async[F].delay(collection.deleteOne(equal("_id", id)).toFuture())).void
  }
}
