package com.magicwinnie.reminder

import cats.effect._
import com.magicwinnie.reminder.bot.Bot
import com.magicwinnie.reminder.db.{MongoDBClient, ReminderModel, ReminderRepository}
import com.magicwinnie.reminder.state.{PerChatState, UserState}
import org.mongodb.scala.MongoCollection

object MainBot extends IOApp {
  private def makeBot[F[_]: Async](token: String, collection: MongoCollection[ReminderModel]): Resource[F, Bot[F]] = {
    for {
      perChatState <- Resource.eval(PerChatState.create[F, UserState])
      repository = new ReminderRepository[F](collection)
    } yield new Bot[F](token, perChatState, repository)
  }

  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(token, mongoUri) =>
        val collection = new MongoDBClient(mongoUri).getCollection[ReminderModel]("reminders")
        makeBot[IO](token, collection).use { bot =>
          bot.startPolling().as(ExitCode.Success)
        }
      case _ =>
        IO.println("Usage: MainBot $botToken $mongoURI").as(ExitCode.Error)
    }
  }
}
