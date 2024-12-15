FROM openjdk:21-slim

WORKDIR /app

COPY ./bot/target/scala-2.13/telegram-reminder-bot.jar /app/telegram-reminder-bot.jar
COPY ./notifier/target/scala-2.13/telegram-reminder-notifier.jar /app/telegram-reminder-notifier.jar
