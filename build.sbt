val flywayPlayVersion = "9.1.0"

val scalaVersion_2_13 = "2.13.14"
val scalaVersion_3 = "3.3.3"

val defaultFlywayVersion = "9.16.0"
val flywayVersion = sys.env.getOrElse("FLYWAY_PLAY_FLYWAY_VERSION", defaultFlywayVersion)

val scalikejdbcVersion = "4.2.1"

val scalatest = "org.scalatest" %% "scalatest" % "3.2.18" % "test"

lazy val commonSettings = Seq(
  organization := "org.flywaydb",
  scalaVersion := scalaVersion_2_13,
  crossScalaVersions := Seq(scalaVersion_2_13, scalaVersion_3),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val `flyway-play` = project
  .in(file("."))
  .settings(commonSettings)
  .settings(nonPublishingSettings)
  .aggregate(plugin, playapp)

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtTwirl)
  .settings(commonSettings)
  .settings(publishingSettings)
  .settings(
    name := "flyway-play",
    version := flywayPlayVersion,
    libraryDependencies ++= Seq(
      "org.playframework" %% "play" % play.core.PlayVersion.current % "provided",
      "org.playframework" %% "play-test" % play.core.PlayVersion.current % "test"
        excludeAll ExclusionRule(organization = "org.specs2"),
      "org.flywaydb" % "flyway-core" % flywayVersion,
      scalatest
    ),
    scalacOptions ++= Seq("-language:_", "-deprecation")
  )

val playAppName = "playapp"
val playAppVersion = "1.0-SNAPSHOT"

lazy val playapp = project
  .in(file("playapp"))
  .enablePlugins(PlayScala)
  .settings(commonSettings)
  .settings(nonPublishingSettings)
  .settings(
    Test / resourceDirectories += baseDirectory.value / "conf",
    version := playAppVersion,
    libraryDependencies ++= Seq(
      guice,
      "com.h2database" % "h2" % "2.2.224",
      "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0",
      "org.playframework" %% "play-test" % play.core.PlayVersion.current % "test"
        excludeAll ExclusionRule(organization = "org.specs2"),
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion % "test",
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion % "test",
      scalatest
    )
  )
  .dependsOn(plugin)
  .aggregate(plugin)

val publishingSettings = Seq(
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomExtra :=
    <url>https://github.com/flyway/flyway-play</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>https://github.com/flyway/flyway-play/blob/master/LICENSE.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:flyway/flyway-play.git</url>
        <connection>scm:git:git@github.com:flyway/flyway-play.git</connection>
      </scm>
      <developers>
        <developer>
          <id>tototoshi</id>
          <name>Toshiyuki Takahashi</name>
          <url>https://tototoshi.github.io</url>
        </developer>
      </developers>
)

val nonPublishingSettings = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  Test / parallelExecution := false
)
