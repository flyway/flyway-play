val scalaVersion_2_11 = "2.11.12"
val scalaVersion_2_12 = "2.12.6"

val flywayPlayVersion = "5.2.0-SNAPSHOT"

val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"

lazy val `flyway-play` = project.in(file("."))
  .settings(
    scalaVersion := scalaVersion_2_11,
    crossScalaVersions := Seq(scalaVersion_2_11, scalaVersion_2_12)
  )
  .aggregate(plugin, playapp)

lazy val plugin = project.in(file("plugin"))
 .enablePlugins(SbtTwirl).settings(
  Seq(
    name := "flyway-play",
    organization := "org.flywaydb",
    version := flywayPlayVersion,
    scalaVersion := scalaVersion_2_11,
    crossScalaVersions := Seq(scalaVersion_2_11, scalaVersion_2_12),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % play.core.PlayVersion.current % "provided",
      "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
        excludeAll ExclusionRule(organization = "org.specs2"),
      "org.flywaydb" % "flyway-core" % "5.1.4",
      scalatest
    ),
    scalacOptions ++= Seq("-language:_", "-deprecation")
  ) ++ publishingSettings :_*
)

val appDependencies = Seq(
  guice,
  "com.h2database" % "h2" % "[1.4,)",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
    excludeAll ExclusionRule(organization = "org.specs2"),
  "org.scalikejdbc" %% "scalikejdbc" % "3.2.1" % "test",
  "org.scalikejdbc" %% "scalikejdbc-config" % "3.2.1" % "test",
  scalatest
)

val playAppName = "playapp"
val playAppVersion = "1.0-SNAPSHOT"

lazy val playapp = project.in(file("playapp"))
.enablePlugins(PlayScala)
.settings(
  resourceDirectories in Test += baseDirectory.value / "conf",
  scalaVersion := scalaVersion_2_11,
  crossScalaVersions := Seq(scalaVersion_2_11, scalaVersion_2_12),
  version := playAppVersion,
  libraryDependencies ++= appDependencies
)
.dependsOn(plugin)
.aggregate(plugin)

val publishingSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomExtra := _pomExtra
)

val _pomExtra =
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
