# Telegram Reminder Bot

## Описание проекта

TODO

## Запуск проекта через Docker

1. Скопировать `.env.sample` и переименовать в `.env`
2. Заполнить `BOT_TOKEN` в `.env` файле
3. Собрать основного бота через `sbt bot/assembly`
4. Собрать рассыльщика уведомление через `sbt notifier/assembly`  
5. Запустить через `docker compose up --build`
