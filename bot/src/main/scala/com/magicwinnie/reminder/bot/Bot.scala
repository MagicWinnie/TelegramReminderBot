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

import scala.util.{Failure, Success, Try}

/** Telegram bot for managing reminders with support for creating, listing, and deleting reminders.
  *
  * @param token
  *   Telegram bot token
  * @param perChatState
  *   Manages user states for different chat sessions
  * @param reminderRepository
  *   Handles database operations for reminder storage
  * @tparam F
  *   Effect type for asynchronous operations
  */
class Bot[F[_]: Async](
  token: String,
  perChatState: PerChatState[F, UserState],
  reminderRepository: ReminderRepository[F]
) extends TelegramBot[F](token, AsyncHttpClientCatsBackend.usingClient[F](asyncHttpClient()))
  with Polling[F]
  with Commands[F]
  with Callbacks[F] {

  /** Handles the /start command to provide a welcome message */
  onCommand("start") { implicit msg =>
    reply(
      "Привет!\n" +
        "Это бот для создания напоминаний.\n" +
        "Введи /help, чтобы увидеть доступные команды"
    ).void
  }

  /** Handles the /help command to display available bot commands */
  onCommand("help") { implicit msg =>
    reply(
      "Доступные команды:\n" +
        "/start - Приветственное сообщение\n" +
        "/help - Показать это сообщение\n" +
        "/add - Создать напоминание\n" +
        "/list - Показать список напоминаний (можно отредактировать и удалить)"
    ).void
  }

  /** Handles the /list command to display user's reminders with pagination */
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

  /** Sends a paginated list of reminders for a specific chat
    *
    * @param chatId
    *   Unique identifier for the chat
    * @param reminders
    *   List of reminders to display
    * @param page
    *   Current page number
    * @param messageToEdit
    *   Optional message ID for editing existing message
    */
  private def sendReminderPage(
    chatId: Long,
    reminders: Seq[ReminderModel],
    page: Int,
    messageToEdit: Option[Int]
  ): F[Unit] = {
    val pageSize = 5 // Maximum reminders per page
    val totalPages = (reminders.size + pageSize - 1) / pageSize
    val start = page * pageSize
    val end = math.min(start + pageSize, reminders.size)
    val pageReminders = reminders.slice(start, end)

    // Create buttons for each reminder
    val reminderButtons = pageReminders.zipWithIndex.map { case (reminder, _) =>
      val timezoneExecuteAt = reminder.executeAt.withZone(DateTimeZone.forID(reminder.timezoneID))
      InlineKeyboardButton.callbackData(
        s"${reminder.name} - ${timezoneExecuteAt.toString("HH:mm dd.MM.yyyy")} (${reminder.timezoneID})",
        s"reminder:${reminder._id}:$chatId"
      )
    }

    // Create navigation buttons for multipage lists
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

  /** Sends action buttons for a selected reminder
    *
    * @param chatId
    *   Unique identifier for the chat
    * @param reminderId
    *   Unique identifier of the selected reminder
    * @param messageToEdit
    *   Optional message ID for editing existing message
    */
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

  /** Handles incoming callback queries from inline buttons */
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

  /** Processes reminder selection callback
    *
    * @param data
    *   Callback data containing reminder and chat identifiers
    */
  private def handleReminderSelection(data: String)(implicit cbq: CallbackQuery): F[Unit] = {
    data.split(":") match {
      case Array(_, reminderId, chatId) =>
        ackCallback(Some("Что хочешь сделать?")).void *>
          (cbq.message match {
            case Some(msg) =>
              sendReminderAction(chatId.toLong, reminderId, Some(msg.messageId))
            case None =>
              ackCallback(Some("Сообщение не существует")).void
          })
      case _ =>
        ackCallback(Some("Некорректный формат данных")).void
    }
  }

  /** Handles pagination navigation for reminder list
    *
    * @param data
    *   Callback data containing page and chat identifiers
    */
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
                cbq.message match {
                  case Some(msg) =>
                    sendReminderPage(
                      chatId,
                      reminders,
                      pageIndex,
                      Some(msg.messageId)
                    )
                  case None =>
                    ackCallback(Some("Сообщение не существует")).void
                }
              }
          case None =>
            ackCallback(Some("Некорректный формат данных")).void
        }
      case _ =>
        ackCallback(Some("Некорректный формат данных")).void
    }
  }

  /** Handles reminder deletion process
    *
    * @param data
    *   Callback data containing reminder identifier
    */
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

  /** Initiates reminder creation process with the /add command */
  onCommand("add") { implicit msg =>
    reply("Введи название нового напоминания", replyToMessageId = Some(msg.messageId)).void >>
      perChatState.setChatState(UserState.AwaitingName)
  }

  /** Processes incoming messages based on current user state */
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

  /** Processes reminder name input
    *
    * @param text
    *   Reminder name entered by user
    */
  private def handleAwaitingName(text: String)(implicit msg: Message): F[Unit] = {
    perChatState.setChatState(UserState.AwaitingDate(text)) >>
      reply(
        "Введи дату в формате HH:MM DD.MM.YYYY ±HHMM (например, 14:30 25.12.2024 +0300)",
        replyToMessageId = Some(msg.messageId)
      ).void
  }

  /** Processes reminder date input
    *
    * @param name
    *   Reminder name
    * @param text
    *   Date string entered by user
    */
  private def handleAwaitingDate(name: String, text: String)(implicit msg: Message): F[Unit] = {
    parseDateTime(text) match {
      case Right(executeAtUtc) =>
        perChatState.setChatState(UserState.AwaitingRepeat(name, executeAtUtc)) >>
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

  /** Processes reminder repeat interval input
    *
    * @param name
    *   Reminder name
    * @param executeAt
    *   Reminder execution datetime
    * @param text
    *   Repeat interval entered by user
    */
  private def handleAwaitingRepeat(name: String, executeAt: DateTime, text: String)(implicit msg: Message): F[Unit] = {
    parseRepeatInterval(text) match {
      case Right(days) =>
        logger.info(s"Saving notification: $name, $executeAt, ${days.getDays}")

        val repeatPeriod = if (days.getDays > 0) Some(days) else None
        val reminder = ReminderModel(
          chatId = msg.chat.id,
          name = name,
          executeAt = executeAt, // MongoDB saves as UTC
          repeatIn = repeatPeriod,
          timezoneID = executeAt.getZone.getID
        )

        reminderRepository.createReminder(reminder) >>
          reply(
            s"Мы сохранили напоминание с названием \"$name\", " +
              s"который исполнится в ${executeAt.toString("HH:mm dd.MM.yyyy")}" +
              s"${repeatPeriod.fold("")(p => s" с периодом в ${p.getDays} дня(ей)")}",
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

  /** Parses a date-time string into a DateTime object with a specific format.
    *
    * @param dateString
    *   The input date-time string to parse
    * @return
    *   Either the parsed DateTime or an error message
    */
  private def parseDateTime(dateString: String): Either[String, DateTime] = {
    val formatter = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy Z")
    Try(formatter.withOffsetParsed().parseDateTime(dateString)).toEither.left
      .map(_ => "Используй формат HH:MM DD.MM.YYYY ±HHMM, например, 14:30 25.12.2024 +0300")
  }

  /** Converts a string representation of days to a Period.
    *
    * @param daysStr
    *   The number of days as a string
    * @return
    *   Either the created Period or an error message
    */
  private def parseRepeatInterval(daysStr: String): Either[String, Period] = {
    Try(daysStr.toInt) match {
      case Success(days) =>
        if (days >= 0) Right(Period.days(days))
        else Left("Число должно быть положительным")
      case Failure(_) =>
        Left("Введи корректное число дней")
    }
  }

  /** Begins polling for the Telegram Bot and logs the start. */
  override def startPolling(): F[Unit] = {
    logger.info("Telegram Bot has started polling ")
    super.startPolling()
  }

  /** Stops the Telegram Bot and logs the shutdown. */
  override def shutdown(): Unit = {
    logger.info("Telegram Bot has stopped")
    super.shutdown()
  }
}
