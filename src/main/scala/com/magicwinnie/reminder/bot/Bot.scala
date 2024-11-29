package com.magicwinnie.reminder.bot

import cats.effect.Async
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import org.asynchttpclient.Dsl.asyncHttpClient
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

class Bot[F[_]: Async](token: String)
  extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
  with Polling[F]
  with Commands[F] {

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  onCommand("start") { implicit msg =>
    reply("Привет!\nЭто бот для создания напоминаний.\nВведи /create, чтобы начать").void
  }

}
