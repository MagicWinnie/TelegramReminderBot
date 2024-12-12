package com.magicwinnie.reminder.db

import cats.effect.{Async, IO}
import cats.syntax.functor._
import com.github.nscala_time.time.Imports.DateTime
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters.{equal, lte}
import org.mongodb.scala.model.Updates.set

/** Repository interface for managing reminders in the database.
  *
  * @tparam F
  *   Type of effect context
  */
trait ReminderRepository[F[_]] {

  /** Creates a new reminder in the database.
    *
    * @param reminder
    *   The reminder to be created
    */
  def createReminder(reminder: ReminderModel): F[Unit]

  /** Retrieves all reminders for a specific chat.
    *
    * @param chatId
    *   The ID of the chat
    * @return
    *   A sequence of reminders associated with the chat
    */
  def getRemindersForChat(chatId: Long): F[Seq[ReminderModel]]

  /** Retrieves reminders scheduled for execution by the given time.
    *
    * @param currentTime
    *   The current time to check reminders against
    * @return
    *   A sequence of reminders to execute
    */
  def getRemindersToExecute(currentTime: DateTime): F[Seq[ReminderModel]]

  /** Updates an existing reminder in the database.
    *
    * @param reminder
    *   The reminder with updated information
    */
  def updateReminder(reminder: ReminderModel): F[Unit]

  /** Deletes a reminder by its unique identifier.
    *
    * @param id
    *   The unique ID of the reminder to delete
    */
  def deleteReminder(id: ObjectId): F[Unit]
}

/** Factory and implementation for ReminderRepository. */
object ReminderRepository {

  /** Live implementation of ReminderRepository backed by a MongoDB collection.
    *
    * @param collection
    *   The MongoDB collection used to manage reminders
    * @tparam F
    *   Type of effect context
    */
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

  /** Creates a ReminderRepository instance.
    *
    * @param collection
    *   The MongoDB collection used to manage reminders
    * @tparam F
    *   Type of effect context
    * @return
    *   A ReminderRepository instance wrapped in an effect
    */
  def make[F[_]: Async](collection: MongoCollection[ReminderModel]): IO[ReminderRepository[F]] = {
    IO.pure(new LiveReminderRepository[F](collection))
  }

}
