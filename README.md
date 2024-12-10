# Telegram Reminder Bot

## Описание проекта

Это бот для Telegram, разработанный на Scala с использованием фреймворка bot4s.telegram и базы данных MongoDB.
Этот бот позволяет пользователям создавать, просматривать и удалять напоминания.
Поддерживается установка времени выполнения напоминаний с учетом часовых поясов, а также возможность повторения
напоминаний через заданные интервалы времени.

## Запуск проекта через Docker

1. Скопировать пример `.env` и переименовать его:
    ```bash
    cp .env.sample .env
    ```
2. Заполнить переменные окружения в `.env` файле:
    - `BOT_TOKEN`: Токен Telegram бота, полученный от [@BotFather](https://t.me/BotFather)
3. Собрать основной бот:
    ```bash
    sbt bot/assembly
    ```
4. Собрать рассыльщика уведомлений:
    ```bash
    sbt notifier/assembly
    ```
5. Запустить сервисы через Docker Compose:
    ```bash
    docker compose up --build -d
    ```
   Это запустит три контейнера: `reminder-mongodb`, `reminder-bot`, `reminder-notifier`.

## Как использовать проект

- `/start`: Приветственное сообщение.
- `/help`: Показать список доступных команд.
- `/add`: Начать процесс создания нового напоминания.
    1. Ввести название напоминания:

       _Пример ввода:_
        ```
        Поесть
        ```
    2. Ввести дату и время с часовым поясом в формате `HH:MM DD.MM.YYYY ±HHMM`:

       _Пример ввода:_
        ```
        14:30 25.12.2024 +0700
        ```

       _Пример ответа:_
        ```
        Введи через сколько дней повторять это напоминание
        ```
    3. Введите интервал повторения в днях:

       _Пример ввода:_
        ```
        4
        ```

       _Пример ответа:_
        ```
        Мы сохранили напоминание с названием "Поесть", который исполнится в 14:30 25.12.2024 с периодом в 4 дня.
        ```
- `/list`: Показать все текущие напоминания.

  _Пример ответа:_
  ```
  Твои напоминания (Страница 1 из 1):

  1. Поесть - 14:30 25.12.2024 - Повторять каждые 4 дня
     [Редактировать] [Удалить]
  ```

## TODO

- [ ] Добавить функционал редактирования напоминаний
- [ ] Сделать более удобный ввод даты, времени и часового пояса
