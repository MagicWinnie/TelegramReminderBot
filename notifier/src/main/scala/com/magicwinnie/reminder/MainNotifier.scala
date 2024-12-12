package com.magicwinnie.reminder

import cats.effect._
import com.magicwinnie.reminder.db.{MongoDBClient, ReminderModel, ReminderRepository}
import com.magicwinnie.reminder.notifier.Notifier

/** Main entry point for the Reminder Notification Service.
  *
  * Initializes the notifier with a Telegram bot token and MongoDB connection.
  */
object MainNotifier extends IOApp {

  /** Configures and starts the reminder notification service.
    *
    * @param args
    *   Command-line arguments (Telegram bot token, MongoDB URI)
    * @return
    *   IO effect representing the application's exit status
    */
  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(token, mongoUri) =>
        val mongoClient = new MongoDBClient(mongoUri)
        for {
          repository <- mongoClient.getCollection[ReminderModel]("reminders").flatMap { collection =>
            ReminderRepository.make[IO](collection)
          }
          notifier = new Notifier[IO](token, repository)
          _ <- notifier.start().as(ExitCode.Success)
        } yield ExitCode.Success
      case _ =>
        IO.println("Usage: MainNotifier $botToken $mongoURI").as(ExitCode.Error)
    }
  }

}
