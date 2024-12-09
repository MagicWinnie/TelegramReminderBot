Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "0.1.0-SNAPSHOT"

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

lazy val root = (project in file("."))
  .settings(
    name := "telegram-reminder-bot",
    assembly / assemblyJarName :=  "telegram-reminder-bot.jar",
    libraryDependencies ++= Dependencies.test.all ++ Dependencies.core.all
  )

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF")       => MergeStrategy.discard
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.filterDistinctLines
  case PathList("META-INF", xs @ _*)             => MergeStrategy.discard
  case PathList("module-info.class")             => MergeStrategy.discard
  case x                                         => MergeStrategy.first
}
