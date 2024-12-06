package com.magicwinnie.reminder.bot

import cats.effect.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.models.Message
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat, Period}
import com.magicwinnie.reminder.state.{AddState, PerChatState}
import org.asynchttpclient.Dsl.asyncHttpClient
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

class Bot[F[_]: Async](token: String, perChatState: PerChatState[F, AddState])
  extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
  with Polling[F]
  with Commands[F]
  with Callbacks[F] {

  // Initialize Logging
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  // Define bot commands
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
    reply("Введи название нового напоминания", replyToMessageId = Some(msg.messageId)).void >>
      perChatState.clearChatState // Clear any existing state when starting a new reminder
  }

  override def receiveMessage(msg: Message): F[Unit] = {
    implicit val implicitMessage: Message = msg

    val action = msg.text match {
      case Some(text) if !text.startsWith("/") =>
        perChatState.withChatState { s =>
          val prevAddState = s.getOrElse(AddState.empty)

          (prevAddState, text) match {
            case (AddState(None, None, None), reminderName) =>
              val newAddState = prevAddState.copy(name = Some(reminderName))
              perChatState.setChatState(newAddState) >>
                reply(
                  "Введи теперь дату в формате HH:MM DD.MM.YYYY",
                  replyToMessageId = Option(msg.messageId)
                ).void

            case (AddState(Some(_), None, None), dateStr) =>
              parseDateTime(dateStr) match {
                case Some(executeAt) =>
                  val newAddState = prevAddState.copy(executeAt = Some(executeAt))
                  perChatState.setChatState(newAddState) >>
                    reply(
                      "Введи через сколько дней повторять это напоминание",
                      replyToMessageId = Option(msg.messageId)
                    ).void
                case None =>
                  reply(
                    "Неверный формат даты. Введи дату в формате HH:MM DD.MM.YYYY",
                    replyToMessageId = Option(msg.messageId)
                  ).void
              }

            case (AddState(Some(name), Some(executeAt), None), daysStr) =>
              Either
                .catchOnly[NumberFormatException](daysStr.toInt)
                .toOption
                .fold(
                  reply("Пожалуйста, введи корректное число дней", replyToMessageId = Option(msg.messageId)).void
                ) { days =>
                  val newAddState = prevAddState.copy(repeatIn = Some(Period.days(days)))
                  perChatState.setChatState(newAddState) >>
                    reply(
                      s"Мы сохранили напоминание с названием \"$name\", " +
                        s"который исполнится в ${executeAt.toString("HH:mm dd.MM.yyyy")} " +
                        s"с периодом в $days дня(ей)",
                      replyToMessageId = Option(msg.messageId)
                    ).void
                }

            case (_, _) =>
              Async[F].unit
          }
        }
      case _ => Async[F].unit
    }

    action >> super.receiveMessage(msg)
  }

  private def parseDateTime(dateString: String): Option[DateTime] = {
    val dateTimeFormatter = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy")
    Either
      .catchNonFatal(dateTimeFormatter.parseDateTime(dateString))
      .toOption
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
