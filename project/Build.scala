import com.typesafe.sbt.SbtScalariform.scalariformSettings
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  lazy val plugin = Project (
    id = "plugin",
    base = file ("plugin"),
    settings = Defaults.defaultSettings ++ Seq (
      name := "play-flyway",
      organization := "com.github.tototoshi",
      scalaVersion := "2.10.0",
      libraryDependencies ++= Seq(
        "play" %% "play" % "2.1.0" % "provided",
        "com.googlecode.flyway" % "flyway-core" % "2.1.1",
        "commons-io" % "commons-io" % "2.4"
      ),
      scalacOptions ++= Seq("-language:_", "-deprecation")
    ) ++ scalariformSettings

  )

  val appDependencies = Seq(
    "org.slf4j" % "slf4j-simple" % "1.7.2",
    "com.github.seratch" %% "scalikejdbc" % "[1.5,)",
    "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.5,)",
    "com.github.seratch" %% "scalikejdbc-play-plugin" % "[1.5,)",
    "com.h2database" % "h2" % "[1.3,)",
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "org.scalatest" %% "scalatest" % "1.9.1" % "test"
  )

  val playAppName = "playapp"
  val playAppVersion = "1.0-SNAPSHOT"

  val playapp =
    play.Project(
      playAppName,
      playAppVersion,
      appDependencies,
      path = file("playapp")
    ).settings(scalariformSettings:_*)
  .settings(
      resourceDirectories in Test <+= baseDirectory / "conf"
    )
      .dependsOn(plugin)
      .aggregate(plugin)

}
