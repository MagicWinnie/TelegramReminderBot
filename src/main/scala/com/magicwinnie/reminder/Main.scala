package com.magicwinnie.reminder

import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import com.magicwinnie.reminder.bot.Bot
import com.magicwinnie.reminder.state.{UserState, PerChatState}

object Main extends IOApp {
  private def makeBot[F[_]: Async](token: String): Resource[F, Bot[F]] =
    Resource.eval(PerChatState.create[F, UserState]).map { perChatState =>
      new Bot[F](token, perChatState)
    }

  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(token) =>
        makeBot[IO](token).use { bot => bot.startPolling().as(ExitCode.Success) }
      case _ =>
        IO.raiseError(new Exception("Usage:\nMain $token"))
    }
  }
}
