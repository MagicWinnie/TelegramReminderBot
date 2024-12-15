package com.magicwinnie.reminder.notifier

import cats.effect.{Async, Concurrent, Temporal}
import cats.syntax.all._
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods.{ParseMode, SendMessage}
import com.bot4s.telegram.models.ChatId
import com.github.nscala_time.time.Imports.{DateTime, DateTimeZone}
import com.magicwinnie.reminder.db.{ReminderModel, ReminderRepository}
import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.Dsl._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.duration._

/** Manages reminder notifications for a Telegram bot.
  *
  * Periodically checks for due reminders and sends notifications to the appropriate chat, with support for recurring
  * reminders.
  *
  * @param token
  *   Telegram bot token
  * @param reminderRepository
  *   Repository for managing reminder data in MongoDB
  */
class Notifier[F[_]: Async](
  token: String,
  reminderRepository: ReminderRepository[F]
) extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
  with StrictLogging {

  /** Continuously processes and sends due reminders.
    *
    * Retrieves due reminders, sends Telegram messages, and handles reminder repetition or deletion.
    */
  private def processReminders(): F[Unit] = {
    def loop: F[Unit] = {
      for {
        currentTime <- Async[F].delay(DateTime.now(DateTimeZone.UTC))
        dueReminders <- reminderRepository.getRemindersToExecute(currentTime)
        _ <- dueReminders.traverse_ { reminder =>
          val messageText = s"🕒 *Напоминание*: ${reminder.name}"

          val send = SendMessage(
            chatId = ChatId(reminder.chatId),
            text = messageText,
            parseMode = Some(ParseMode.MarkdownV2)
          )

          request(send).flatMap { message =>
            Concurrent[F].delay(
              logger.info(s"Message sent: '${message.text.getOrElse("")}' to chatId: ${message.chat.id}")
            )
          } *> handleRepeat(reminder)
        }
        _ <- Temporal[F].sleep(30.seconds)
        _ <- loop
      } yield ()
    }

    loop.handleErrorWith { e =>
      Concurrent[F].delay(logger.info(s"Error in reminder processor: ${e.getMessage}")) *>
        Temporal[F].sleep(30.seconds) *>
        processReminders()
    }
  }

  /** Manages the reminder after notification.
    *
    * Updates recurring reminders with a new execution time, or deletes non-recurring reminders.
    *
    * @param reminder
    *   The reminder to process after notification
    */
  private def handleRepeat(reminder: ReminderModel): F[Unit] = {
    reminder.repeatIn match {
      case Some(period) =>
        val newExecuteAt = reminder.executeAt.plus(period)
        reminderRepository.updateReminder(reminder.copy(executeAt = newExecuteAt))
      case None =>
        reminderRepository.deleteReminder(reminder._id)
    }
  }

  /** Initiates the reminder notification process. */
  def start(): F[Unit] = {
    logger.info("Notifier has started processing reminders")
    processReminders()
  }

}
