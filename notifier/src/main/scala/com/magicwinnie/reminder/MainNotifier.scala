package com.magicwinnie.reminder

import cats.effect._
import com.magicwinnie.reminder.db.{MongoDBClient, ReminderModel, ReminderRepository}
import com.magicwinnie.reminder.notifier.Notifier

object MainNotifier extends IOApp {

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
