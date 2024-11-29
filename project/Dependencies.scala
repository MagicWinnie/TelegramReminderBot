import sbt.*

object Dependencies {

  object core {
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.7"
    lazy val telegramCore = "com.bot4s" %% "telegram-core" % "5.8.3"
    lazy val mongoScalaDriver = "org.mongodb.scala" %% "mongo-scala-driver" % "5.2.0"

    lazy val slogging = "biz.enef" %% "slogging" % "0.6.2"
    lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.13"

    lazy val sttpCore = "com.softwaremill.sttp.client3" %% "core" % "3.9.7"
    lazy val sttpAsync = "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.9.7"

    lazy val all: Seq[ModuleID] =
      Seq(catsEffect, telegramCore, mongoScalaDriver, slogging, slf4jSimple, sttpCore, sttpAsync)
  }

  object test {
    lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19" % Test
//    lazy val catsLaws = "org.typelevel" %% "cats-laws" % "2.12.0" % Test
//    lazy val disciplineScalatest = "org.typelevel" %% "discipline-scalatest" % "2.3.0" % Test

    lazy val all: Seq[ModuleID] = Seq(scalaTest)
  }
}
