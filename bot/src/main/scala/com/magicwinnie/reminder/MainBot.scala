package com.magicwinnie.reminder

import cats.effect._
import com.magicwinnie.reminder.bot.Bot
import com.magicwinnie.reminder.db.{MongoDBClient, ReminderModel, ReminderRepository}
import com.magicwinnie.reminder.state.{PerChatState, UserState}

/** Main entry point for the Reminder Telegram Bot application.
  *
  * Initializes the bot with a Telegram token and MongoDB connection.
  */
object MainBot extends IOApp {

  /** Configures and starts the Telegram Bot with MongoDB integration.
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
          perChatState <- PerChatState.create[IO, UserState]
          repository <- mongoClient.getCollection[ReminderModel]("reminders").flatMap { collection =>
            ReminderRepository.make[IO](collection)
          }
          exitCode <- new Bot[IO](token, perChatState, repository).startPolling().as(ExitCode.Success)
        } yield exitCode
      case _ =>
        IO.println("Usage: MainBot $botToken $mongoURI").as(ExitCode.Error)
    }
  }

}
