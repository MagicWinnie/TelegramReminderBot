#FROM openjdk:21-slim as build
#
#WORKDIR /app
#
#RUN apt update && apt install -y curl
#
#RUN curl -sL https://github.com/sbt/sbt/releases/download/v1.10.3/sbt-1.10.3.tgz | tar xz && mv sbt*/bin/sbt /usr/local/bin/sbt
#
#COPY build.sbt .
#COPY project/ ./project/
#COPY src/ ./src/
#
#RUN sbt -debug assembly

FROM openjdk:21-slim

WORKDIR /app

#COPY --from=build /app/target/scala-2.13/telegram-reminder-bot.jar /app/telegram-reminder-bot.jar

COPY target/scala-2.13/telegram-reminder-bot.jar /app/telegram-reminder-bot.jar

COPY entrypoint.sh .
RUN chmod +x ./entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
