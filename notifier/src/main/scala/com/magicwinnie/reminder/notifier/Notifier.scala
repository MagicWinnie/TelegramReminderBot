package com.magicwinnie.reminder.notifier

import cats.effect.{Async, Concurrent, Temporal}
import cats.syntax.all._
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods.{ParseMode, SendMessage}
import com.bot4s.telegram.models.ChatId
import com.github.nscala_time.time.Imports.{DateTime, DateTimeZone}
import com.magicwinnie.reminder.db.{ReminderModel, ReminderRepository}
import org.asynchttpclient.Dsl._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.duration._

class Notifier[F[_]: Async](
  token: String,
  reminderRepository: ReminderRepository[F]
) extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient())) {

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
              println(s"Message sent: '${message.text.getOrElse("")}' to chatId: ${message.chat.id}")
            )
          } *> handleRepeat(reminder)
        }
        _ <- Temporal[F].sleep(30.seconds)
        _ <- loop
      } yield ()
    }

    loop.handleErrorWith { e =>
      Concurrent[F].delay(println(s"Error in reminder processor: ${e.getMessage}")) *>
        Temporal[F].sleep(30.seconds) *>
        processReminders()
    }
  }

  private def handleRepeat(reminder: ReminderModel): F[Unit] = {
    reminder.repeatIn match {
      case Some(period) =>
        val newExecuteAt = reminder.executeAt.plus(period)
        reminderRepository.updateReminder(reminder.copy(executeAt = newExecuteAt))
      case None =>
        reminderRepository.deleteReminder(reminder._id)
    }
  }

  def start(): F[Unit] = {
    println("Notifier has started processing reminders")
    processReminders()
  }

}
