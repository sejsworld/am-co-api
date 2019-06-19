import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
name := """amazing-co-api"""
organization := "co.amazing"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test
libraryDependencies ++= Seq(
  ehcache
  
)

// Docker setup after this:
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

packageName in Docker := name.value
version in Docker := version.value
maintainer in Docker := "sejensen@gmail.com"

dockerUpdateLatest := true
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:11-jre-slim"

dockerBaseImage := "openjdk:latest"


dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "mkdir", "-p", "/var/data/"),
  ExecCmd("RUN", "chown", "-R", "daemon:daemon", "/var/data/"),
  Cmd("USER", "daemon")
)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "co.amazing.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "co.amazing.binders._"
