package com.magicwinnie.reminder.bot

import cats.effect.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.models.Message
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat, Period}
import com.magicwinnie.reminder.state.{AddState, PerChatState}
import org.asynchttpclient.Dsl.asyncHttpClient
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

class Bot[F[_]: Async](token: String)
  extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
  with Polling[F]
  with Commands[F]
  with Callbacks[F]
  with PerChatState[AddState] {

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

  onCommand("add") { implicit msg =>
    reply("Введи название нового напоминания", replyToMessageId = Option(msg.messageId)).void
  }

  def processState(implicit msg: Message): F[Unit] = {
    withChatState[F] { _ =>
      val newAddState = AddState(msg.text, None, None)
      setChatState(newAddState)
    }
  }

  override def receiveMessage(msg: Message): F[Unit] = {
    implicit val implicitMessage: Message = msg

    val action = msg.text match {
      case Some(text) if !text.startsWith("/") =>
        withChatState { s =>
          val prevAddState = s.getOrElse(AddState(None, None, None))
          if (prevAddState.name.isEmpty) {
            val newAddState = AddState(Some(text), None, None)
            setChatState(newAddState) >>
              reply("Введи теперь дату в формате HH:MM DD.MM.YYYY", replyToMessageId = Option(msg.messageId)).void
          } else if (prevAddState.executeAt.isEmpty) {
            val executeAt = parseDateTime(text)
            if (executeAt.isEmpty) {
              reply("Введи теперь дату в формате HH:MM DD.MM.YYYY", replyToMessageId = Option(msg.messageId)).void
            } else {
              val newAddState = AddState(prevAddState.name, executeAt, None)
              setChatState(newAddState) >>
                reply(
                  "Введи через сколько дней повторять это напоминание",
                  replyToMessageId = Option(msg.messageId)
                ).void
            }
          } else {
            val newAddState = AddState(prevAddState.name, prevAddState.executeAt, Some(Period.days(text.toInt)))
            setChatState(newAddState) >>
              reply(
                s"Мы сохранили напоминание с названием \"${newAddState.name.getOrElse("")}\"," +
                  s" который исполнится в ${newAddState.executeAt.getOrElse("")} с периодом в" +
                  s" ${text.toInt} дня",
                replyToMessageId = Option(msg.messageId)
              ).void
          }
        }
      case _ => Async[F].unit
    }

    action >> super.receiveMessage(msg)
  }

  private def parseDateTime(dateString: String): Option[DateTime] = {
    try {
      val dateTimeFormatter = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy")
      Some(dateTimeFormatter.parseDateTime(dateString))
    } catch {
      case _: IllegalArgumentException =>
        None
    }
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
