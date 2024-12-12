package com.magicwinnie.reminder

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.magicwinnie.reminder.db.{MongoDBClient, ReminderModel, ReminderRepository}
import com.magicwinnie.reminder.notifier.Notifier

object MainNotifier extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(token, mongoUri) =>
        val collection = new MongoDBClient(mongoUri).getCollection[ReminderModel]("reminders")
        val repository = new ReminderRepository[IO](collection)

        val notifier = new Notifier[IO](token, repository)

        val program = for {
          _ <- Resource.eval(notifier.start())
        } yield ExitCode.Success

        program.use(_ => IO.never).as(ExitCode.Success).handleErrorWith { e =>
          IO.println(s"Error starting notifier: ${e.getMessage}").as(ExitCode.Error)
        }

      case _ =>
        IO.println("Usage: MainNotifier $botToken $mongoURI").as(ExitCode.Error)
    }
  }
}
