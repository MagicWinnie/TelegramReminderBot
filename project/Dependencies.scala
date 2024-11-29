import sbt.*

object Dependencies {

  object core {
//    lazy val catsCore = "org.typelevel" %% "cats-core" % "2.12.0"
    lazy val telegramCore = "com.bot4s" %% "telegram-core" % "5.8.3"
    lazy val mongoScalaDriver = "org.mongodb.scala" %% "mongo-scala-driver" % "5.2.0"
    lazy val slogging = "biz.enef" %% "slogging" % "0.6.2"
    lazy val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.13"

    lazy val all: Seq[ModuleID] = Seq(telegramCore, mongoScalaDriver, slogging, slf4jSimple)
  }

  object test {
    lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19" % Test
//    lazy val catsLaws = "org.typelevel" %% "cats-laws" % "2.12.0" % Test
//    lazy val disciplineScalatest = "org.typelevel" %% "discipline-scalatest" % "2.3.0" % Test

    lazy val all: Seq[ModuleID] = Seq(scalaTest)
  }
}
