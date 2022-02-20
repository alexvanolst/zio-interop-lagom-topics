ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.github.alexvanolst"
ThisBuild / organizationName := "Alexander van Olst"
// ThisBuild / scalacOptions += "-Ymacro-debug-lite"

lazy val root = (project in file("."))
  .settings(
    name := "zio-lagom-interop",
    libraryDependencies ++= Seq(
      "dev.zio"              %% "zio"                % "1.0.13",
      "io.github.kitlangton" %% "zio-magic"          % "0.3.11",
      "com.lightbend.lagom"  %% "lagom-scaladsl-api" % "1.6.7",
      "dev.zio"              %% "zio-kafka"          % "0.17.4",
      "ch.qos.logback"       % "logback-classic"     % "1.1.3" % Runtime,
      "org.scala-lang"       % "scala-reflect"       % scalaVersion.value,
      "dev.zio"              %% "zio-test"           % "1.0.13" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
