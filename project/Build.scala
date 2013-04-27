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
      version := "0.1.3-SNAPSHOT",
      scalaVersion := "2.10.0",
      libraryDependencies ++= Seq(
        "play" %% "play" % "2.1.1" % "provided",
        "com.googlecode.flyway" % "flyway-core" % "2.1.1",
        "org.scalatest" %% "scalatest" % "1.9.1" % "test"
      ),
      scalacOptions ++= Seq("-language:_", "-deprecation")
    ) ++ scalariformSettings ++ publishingSettings

  )

  val appDependencies = Seq(
    "com.h2database" % "h2" % "[1.3,)",
    "org.slf4j" % "slf4j-simple" % "1.7.2" % "test",
    "com.github.seratch" %% "scalikejdbc" % "1.5.2" % "test",
    "com.github.seratch" %% "scalikejdbc-interpolation" % "1.5.2" % "test",
    "com.github.seratch" %% "scalikejdbc-play-plugin" % "1.5.2" % "test",
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
  .settings(resourceDirectories in Test <+= baseDirectory / "conf")
  .dependsOn(plugin)
  .aggregate(plugin)

  val publishingSettings = Seq(
    publishMavenStyle := true,
    publishTo <<= version { (v: String) => _publishTo(v) },
    publishArtifact in Test := false,
    pomExtra := _pomExtra
  )

  def _publishTo(v: String) = {
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }

  val _pomExtra =
    <url>http://github.com/tototoshi/play-flyway</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://github.com/tototoshi/play-flyway/blob/master/LICENSE.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:tototoshi/play-flyway.git</url>
      <connection>scm:git:git@github.com:tototoshi/play-flyway.git</connection>
    </scm>
    <developers>
      <developer>
        <id>tototoshi</id>
        <name>Toshiyuki Takahashi</name>
        <url>http://tototoshi.github.com</url>
      </developer>
    </developers>

}
