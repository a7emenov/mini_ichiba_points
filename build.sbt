import com.typesafe.sbt.packager.docker._

name := "mini_ichiba_points"

version := "0.6"
scalaVersion := "2.12.9"

// Compiler options
scalacOptions ++= Seq(
  "-language:higherKinds",
  "-Ypartial-unification"
)


// Docker deployment
enablePlugins(JavaAppPackaging)
dockerBaseImage := "openjdk:8-jre-alpine"
version in Docker := "latest"
packageName in Docker := "mini-ichiba-points"

dockerCommands in Docker ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
)

dockerExposedPorts in Docker ++= Seq(8080)
dockerUpdateLatest in Docker := true

// Dependencies
val Version = new {
  val monix = "3.0.0-RC3"
  val http4s = "0.21.0-M1"
  val tsec = "0.2.0-M1"
  val circe = "0.11.1"
  val postgresDriver = "42.2.6"
  val quill = "3.4.3"
  val pureconfig = "0.11.1"
  val scalaLogging = "3.9.2"
  val logback = "1.2.3"
  val flyway = "6.0.1"
}

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % Version.monix,
  
  "org.http4s" %% "http4s-dsl" % Version.http4s,
  "org.http4s" %% "http4s-blaze-server" % Version.http4s,
  "org.http4s" %% "http4s-circe" % Version.http4s,

  "io.github.jmcardon" %% "tsec-common" % Version.tsec,
  "io.github.jmcardon" %% "tsec-http4s" % Version.tsec,
  "io.github.jmcardon" %% "tsec-jwt-mac" % Version.tsec,

  "io.circe" %% "circe-generic" % Version.circe,
  
  "org.postgresql" % "postgresql" % Version.postgresDriver,
  "io.getquill" %% "quill-jdbc-monix" % Version.quill,

  "com.github.pureconfig" %% "pureconfig" % Version.pureconfig,

  "com.typesafe.scala-logging" %% "scala-logging" % Version.scalaLogging,
  "ch.qos.logback" % "logback-classic" % Version.logback,

  "org.flywaydb" % "flyway-core" % Version.flyway
)

// Compiler plugins
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")