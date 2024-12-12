import sbt.*

object Dependencies {

  object core {
    lazy val all: Seq[ModuleID] = Seq(
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "com.bot4s" %% "telegram-core" % "5.8.3",
      "org.mongodb.scala" %% "mongo-scala-driver" % "5.1.1",
      "io.circe" %% "circe-generic" % "0.14.9",
      "io.circe" %% "circe-parser" % "0.14.9",
      "org.slf4j" % "slf4j-simple" % "2.0.13",
      "com.softwaremill.sttp.client3" %% "core" % "3.9.7",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.9.7",
      "com.github.nscala-time" %% "nscala-time" % "2.34.0"
    )
  }

  object test {
    lazy val all: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.mockito" %% "mockito-scala" % "1.17.37",
      "org.scalatestplus" %% "mockito-5-12" % "3.2.19.0" % Test,
      "org.testcontainers" % "mongodb" % "1.20.0",
    )
  }
}
