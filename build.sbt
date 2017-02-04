import AssemblyKeys._
scalaVersion := "2.12.1"
mainClass in assembly := Some("com.victor.game.war.EntryPoint")
jarName in assembly := "war.jar"
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "EntryPoint",
    libraryDependencies +="com.typesafe.akka" % "akka-actor_2.12" % "2.4.16"

  ).settings(assemblySettings: _*)

