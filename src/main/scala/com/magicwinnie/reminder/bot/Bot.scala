package com.magicwinnie.reminder.bot

import cats.effect.Async
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.models.Message
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

  onCommand("/inc") { implicit msg =>
    withChatState { s =>
      val prevAddState = s.getOrElse(AddState(None, None, None))
      val newAddState = AddState(Some("234"), None, None)
      setChatState(newAddState)
      reply(s"Counter: ${prevAddState.name}").void
    }
  }

  def processState(implicit msg: Message): F[Unit] = {
    withChatState[F] { _ =>
      val newAddState = AddState(msg.text, None, None)
      setChatState(newAddState)
    }
  }

  override def receiveMessage(msg: Message): F[Unit] = {
//    msg.text match {
//      case Some(text) =>
//        if (!text.startsWith("/")) {
//          withChatState { s =>
//            val prevAddState = s.getOrElse(AddState(None, None, None))
//            if (prevAddState.name.isEmpty) {
//              val newAddState = AddState(Some(text), None, None)
//              setChatState(newAddState)(msg)
//            } else if (prevAddState.executeAt.isEmpty) {
//              val newAddState = AddState(prevAddState.name, Some(text), None)
//              setChatState(newAddState)(msg)
//            } else {
//              val newAddState = AddState(prevAddState.name, Some(text), None)
//              setChatState(newAddState)(msg)
//            }
//          }(msg)
//        }
//      case None => ()
//    }
    super.receiveMessage(msg)
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
