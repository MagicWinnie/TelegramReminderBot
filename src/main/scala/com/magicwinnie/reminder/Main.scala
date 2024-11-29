package com.magicwinnie.reminder

import cats.effect.{ExitCode, IO, IOApp}
import com.magicwinnie.reminder.bot.Bot

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(token) =>
        new Bot[IO](token).startPolling().map(_ => ExitCode.Success)
      case _ =>
        IO.raiseError(new Exception("Usage:\nMain $token"))
    }
  }
}
