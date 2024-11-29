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
    reply(
      "Привет!\n" +
        "Это бот для создания напоминаний.\n" +
        "Введи /help, чтобы увидеть доступные команды"
    ).void
  }

  onCommand("help") { implicit msg =>
    reply(
      "Доступные команды:\n" +
        "/start - Приветственное сообщение\n" +
        "/help - Показать это сообщение\n" +
        "/add - Создать напоминание\n" +
        "/list - Показать список напоминаний (можно отредактировать и удалить)"
    ).void
  }

  override def startPolling(): F[Unit] = {
    logger.info("Telegram Bot has started polling ")
    super.startPolling()
  }

  override def shutdown(): Unit = {
    logger.info("Telegram Bot has stopped")
    super.shutdown()
  }
}
