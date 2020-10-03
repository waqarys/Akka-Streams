import sbt._

lazy val base = (project in file("."))
  .aggregate(
    common,
    exercises
  )
  .settings(CommonSettings.commonSettings: _*)

lazy val common = project.settings(CommonSettings.commonSettings: _*)

lazy val exercises = project
  .settings(CommonSettings.commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")
  .enablePlugins(Cinnamon)

(Compile / runMain) := ((Compile / runMain) in exercises).evaluated
onLoad in Global := (onLoad in Global).value andThen (Command.process("project exercises", _))
