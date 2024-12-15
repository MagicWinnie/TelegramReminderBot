ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "0.1.0-SNAPSHOT"

Global / onChangedBuildSource := ReloadOnSourceChanges

Compile / compile / scalacOptions ++= Seq(
  "-Werror",
  "-Wdead-code",
  "-Wextra-implicit",
  "-Wnumeric-widen",
  "-Wunused",
  "-Wvalue-discard",
  "-Xlint",
  "-Xlint:-byname-implicit",
  "-Xlint:-implicit-recursion",
  "-unchecked"
)

lazy val commonAssemblySettings = Seq(
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "MANIFEST.MF")       => MergeStrategy.discard
    case PathList("META-INF", "services", xs @ _*) => MergeStrategy.filterDistinctLines
    case PathList("META-INF", xs @ _*)             => MergeStrategy.discard
    case PathList("module-info.class")             => MergeStrategy.discard
    case x                                         => MergeStrategy.first
  },
  assembly / assemblyJarName := {
    s"${name.value}.jar"
  }
)

lazy val common = (project in file("common"))
  .settings(
    name := "telegram-reminder-common",
    libraryDependencies ++= Dependencies.test.all ++ Dependencies.core.all
  )

lazy val bot = (project in file("bot"))
  .dependsOn(common)
  .settings(
    name := "telegram-reminder-bot",
    libraryDependencies ++= Dependencies.test.all ++ Dependencies.core.all
  )
  .settings(commonAssemblySettings)

lazy val notifier = (project in file("notifier"))
  .dependsOn(common)
  .settings(
    name := "telegram-reminder-notifier",
    libraryDependencies ++= Dependencies.test.all ++ Dependencies.core.all
  )
  .settings(commonAssemblySettings)

lazy val root = (project in file("."))
  .aggregate(common, bot, notifier)
  .settings(
    name := "telegram-reminder-bot-project"
  )
