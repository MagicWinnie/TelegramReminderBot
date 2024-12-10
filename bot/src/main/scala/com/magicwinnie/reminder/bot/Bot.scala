package com.magicwinnie.reminder.bot

import cats.effect.Async
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.methods.{EditMessageText, SendMessage}
import com.bot4s.telegram.models._
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat, DateTimeZone, Period}
import com.magicwinnie.reminder.db.{ReminderModel, ReminderRepository}
import com.magicwinnie.reminder.state.{PerChatState, UserState}
import org.asynchttpclient.Dsl.asyncHttpClient
import org.bson.types.ObjectId
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.util.Try

class Bot[F[_]: Async](
  token: String,
  perChatState: PerChatState[F, UserState],
  reminderRepository: ReminderRepository[F]
) extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
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
    for {
      chatId <- Async[F].pure(msg.chat.id)
      reminders <- reminderRepository.getRemindersForChat(chatId)
      _ <-
        if (reminders.isEmpty)
          reply("Нет напоминаний", replyToMessageId = Some(msg.messageId))
        else
          sendReminderPage(chatId, reminders, 0, None)
    } yield ()
  }

  private def sendReminderPage(
    chatId: Long,
    reminders: Seq[ReminderModel],
    page: Int,
    messageToEdit: Option[Int]
  ): F[Unit] = {
    val pageSize = 5
    val totalPages = (reminders.size + pageSize - 1) / pageSize
    val start = page * pageSize
    val end = math.min(start + pageSize, reminders.size)
    val pageReminders = reminders.slice(start, end)

    val reminderButtons = pageReminders.zipWithIndex.map { case (reminder, _) =>
      InlineKeyboardButton.callbackData(reminder.name, s"reminder:${reminder._id}:$chatId")
    }

    val navButtons = List(
      if (page > 0) Some(InlineKeyboardButton.callbackData("⬅️ Назад", s"page:${page - 1}:$chatId")) else None,
      if (page < totalPages - 1) Some(InlineKeyboardButton.callbackData("➡️ Далее", s"page:${page + 1}:$chatId"))
      else None
    ).flatten

    val keyboard = InlineKeyboardMarkup(
      reminderButtons.grouped(1).toList ++ navButtons.grouped(2).toList
    )

    val messageText =
      s"Твои напоминания (Страница ${page + 1} из $totalPages):"

    messageToEdit match {
      case None =>
        request(
          SendMessage(
            text = messageText,
            chatId = ChatId(chatId),
            replyMarkup = Some(keyboard)
          )
        ).void

      case _ =>
        request(
          EditMessageText(
            text = messageText,
            chatId = Some(ChatId(chatId)),
            messageId = messageToEdit,
            replyMarkup = Some(keyboard)
          )
        ).void
    }
  }

  private def sendReminderAction(chatId: Long, reminderId: String, messageToEdit: Option[Int]): F[Unit] = {
    val keyboard = InlineKeyboardMarkup.singleRow(
      Seq(
        InlineKeyboardButton.callbackData("Редактировать", s"edit:$reminderId:$chatId"),
        InlineKeyboardButton.callbackData("Удалить", s"delete:$reminderId:$chatId")
      )
    )

    val messageText = "Что хочешь сделать с этим напоминанием?"

    messageToEdit match {
      case None =>
        request(
          SendMessage(
            text = messageText,
            chatId = ChatId(chatId),
            replyMarkup = Some(keyboard)
          )
        ).void

      case Some(messageId) =>
        request(
          EditMessageText(
            text = messageText,
            chatId = Some(ChatId(chatId)),
            messageId = Some(messageId),
            replyMarkup = Some(keyboard)
          )
        ).void
    }
  }

  onCallbackQuery { implicit cbq =>
    cbq.data match {
      case Some(data) if data.startsWith("reminder:") =>
        handleReminderSelection(data)
      case Some(data) if data.startsWith("page:") =>
        handlePageNavigation(data)
      case Some(data) if data.startsWith("delete:") =>
        handleDeleteReminder(data)
      // TODO: add "edit:"
      case _ =>
        ackCallback(Some("Некорректный запрос")).void
    }
  }

  private def handleReminderSelection(data: String)(implicit cbq: CallbackQuery): F[Unit] = {
    data.split(":") match {
      case Array(_, reminderId, chatId) =>
        ackCallback(Some("Что хочешь сделать?")).void *>
          sendReminderAction(chatId.toLong, reminderId, Some(cbq.message.get.messageId)) // TODO: fix unsafe .get
      case _ =>
        ackCallback(Some("Некорректный формат данных")).void
    }
  }

  private def handlePageNavigation(data: String)(implicit cbq: CallbackQuery): F[Unit] = {
    data.split(":") match {
      case Array(_, pageIndexStr, chatIdStr) =>
        (for {
          pageIndex <- Try(pageIndexStr.toInt).toOption
          chatId <- Try(chatIdStr.toLong).toOption
        } yield (pageIndex, chatId)) match {
          case Some((pageIndex, chatId)) =>
            ackCallback(Some("Переключение страницы...")).void *>
              reminderRepository.getRemindersForChat(chatId).flatMap { reminders =>
                sendReminderPage(
                  chatId,
                  reminders,
                  pageIndex,
                  Some(cbq.message.get.messageId)
                ) // TODO: fix unsafe .get
              }
          case None =>
            ackCallback(Some("Некорректный формат данных")).void
        }
      case _ =>
        ackCallback(Some("Некорректный формат данных")).void
    }
  }

  private def handleDeleteReminder(data: String)(implicit cbq: CallbackQuery): F[Unit] = {
    data.split(":") match {
      case Array(_, reminderId, _) =>
        if (ObjectId.isValid(reminderId)) {
          logger.info(s"Deleting reminder with id $reminderId")
          val objectId = new ObjectId(reminderId)
          for {
            _ <- reminderRepository.deleteReminder(objectId)
            _ <- ackCallback(Some("Напоминание удалено"))
            _ <- cbq.message.traverse(msg =>
              request(
                EditMessageText(
                  chatId = Some(ChatId(msg.chat.id)),
                  messageId = Some(msg.messageId),
                  text = "Напоминание удалено"
                )
              )
            )
          } yield ()
        } else {
          ackCallback(Some("Неверный формат ID напоминания")).void
        }
      case _ =>
        ackCallback(Some("Некорректный формат данных")).void
    }
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
        logger.info(s"Saving notification: $name, ${executeAt.toString("HH:mm dd.MM.yyyy")}, ${days.getDays}")

        val repeatPeriod = if (days.getDays > 0) Some(days) else None
        val reminder = ReminderModel(
          chatId = msg.chat.id,
          name = name,
          executeAt = executeAt,
          repeatIn = repeatPeriod,
        )

        reminderRepository.createReminder(reminder) >>
          reply(
            s"Мы сохранили напоминание с названием \"$name\", " +
              s"который исполнится в ${executeAt.toString("HH:mm dd.MM.yyyy")} " +
              s"${repeatPeriod.fold("")(p => s"с периодом в ${p.getDays} дня(ей)")}",
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
    Either.catchNonFatal(DateTime.parse(dateString, formatter)).left.map(_ => "Используй формат HH:MM DD.MM.YYYY")
  }

  private def parseRepeatInterval(daysStr: String): Either[String, Period] = {
    Either
      .catchOnly[NumberFormatException](daysStr.toInt)
      .left
      .map(_ => "Введи корректное число дней")
      .flatMap { days =>
        if (days >= 0) Right(Period.days(days))
        else Left("Число должно быть положительным")
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
