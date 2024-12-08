package com.magicwinnie.reminder.bot

import cats.effect.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.models.Message
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat, Period}
import com.magicwinnie.reminder.state.{PerChatState, UserState}
import org.asynchttpclient.Dsl.asyncHttpClient
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

class Bot[F[_]: Async](token: String, perChatState: PerChatState[F, UserState])
  extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
  with Polling[F]
  with Commands[F]
  with Callbacks[F] {

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

  onCommand("list") { implicit msg =>
    reply("Команда находится в разработке", replyToMessageId = Some(msg.messageId)).void
  }

  onCommand("add") { implicit msg =>
    reply("Введи название нового напоминания", replyToMessageId = Some(msg.messageId)).void >>
      perChatState.setChatState(UserState.AwaitingName)
  }

  override def receiveMessage(msg: Message): F[Unit] = {
    implicit val implicitMessage: Message = msg

    (msg.text match {
      case Some(text) if !text.startsWith("/") =>
        perChatState.withChatState {
          case Some(UserState.AwaitingName) =>
            handleAwaitingName(text)
          case Some(UserState.AwaitingDate(name)) =>
            handleAwaitingDate(name, text)
          case Some(UserState.AwaitingRepeat(name, executeAt)) =>
            handleAwaitingRepeat(name, executeAt, text)
          case _ =>
            Async[F].unit
        }

      case _ =>
        Async[F].unit
    }) >> super.receiveMessage(msg)
  }

  private def handleAwaitingName(text: String)(implicit msg: Message): F[Unit] = {
    perChatState.setChatState(UserState.AwaitingDate(text)) >>
      reply(
        "Введи дату в формате HH:MM DD.MM.YYYY",
        replyToMessageId = Some(msg.messageId)
      ).void
  }

  private def handleAwaitingDate(name: String, text: String)(implicit msg: Message): F[Unit] = {
    parseDateTime(text) match {
      case Right(executeAt) =>
        perChatState.setChatState(UserState.AwaitingRepeat(name, executeAt)) >>
          reply(
            "Введи через сколько дней повторять это напоминание",
            replyToMessageId = Some(msg.messageId)
          ).void
      case Left(errorMsg) =>
        reply(
          s"Неверный формат даты. $errorMsg",
          replyToMessageId = Some(msg.messageId)
        ).void
    }
  }

  private def handleAwaitingRepeat(name: String, executeAt: DateTime, text: String)(implicit msg: Message): F[Unit] = {
    parseRepeatInterval(text) match {
      case Right(days) =>
        // TODO: save to DB
        logger.info(s"Saving notification: $name, ${executeAt.toString("HH:mm dd.MM.yyyy")}, ${days.getDays}")
        reply(
          s"Мы сохранили напоминание с названием \"$name\", " +
            s"который исполнится в ${executeAt.toString("HH:mm dd.MM.yyyy")} " +
            s"с периодом в ${days.getDays} дня(ей)",
          replyToMessageId = Some(msg.messageId)
        ).void >>
          perChatState.clearChatState
      case Left(errorMsg) =>
        reply(
          s"Неверный формат периода. $errorMsg",
          replyToMessageId = Some(msg.messageId)
        ).void
    }
  }

  private def parseDateTime(dateString: String): Either[String, DateTime] = {
    val formatter = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy")
    Either
      .catchNonFatal(DateTime.parse(dateString, formatter))
      .left
      .map(_ => "Используй формат HH:MM DD.MM.YYYY.")
  }

  private def parseRepeatInterval(daysStr: String): Either[String, Period] = {
    Either
      .catchOnly[NumberFormatException](daysStr.toInt)
      .left
      .map(_ => "Введи корректное число дней.")
      .flatMap { days =>
        if (days > 0) Right(Period.days(days))
        else Left("Число должно быть положительным.")
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
