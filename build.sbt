val scalaVersion_2_11 = "2.11.12"
val scalaVersion_2_12 = "2.12.8"

val flywayVersion = "5.2.4"
val flywayPlayVersion = "5.3.1-SNAPSHOT"

val scalatest = "org.scalatest" %% "scalatest" % "3.0.7" % "test"

lazy val commonSettings = Seq(
  organization := "org.flywaydb",
  scalaVersion := scalaVersion_2_11,
  crossScalaVersions := Seq(scalaVersion_2_11, scalaVersion_2_12),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val `flyway-play` = project.in(file("."))
  .settings(commonSettings)
  .settings(nonPublishingSettings)
  .aggregate(plugin, playapp)

lazy val plugin = project.in(file("plugin"))
  .enablePlugins(SbtTwirl)
  .settings(commonSettings)
  .settings(publishingSettings)
  .settings(
    Seq(
      name := "flyway-play",
      version := flywayPlayVersion,
      resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % play.core.PlayVersion.current % "provided",
        "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
          excludeAll ExclusionRule(organization = "org.specs2"),
        "org.flywaydb" % "flyway-core" % flywayVersion,
        scalatest
      ),
      scalacOptions ++= Seq("-language:_", "-deprecation")
    )
  )

val playAppName = "playapp"
val playAppVersion = "1.0-SNAPSHOT"

lazy val playapp = project.in(file("playapp"))
  .enablePlugins(PlayScala)
  .settings(commonSettings)
  .settings(nonPublishingSettings)
  .settings(
    resourceDirectories in Test += baseDirectory.value / "conf",
    version := playAppVersion,
    libraryDependencies ++= Seq(
      guice,
      "com.h2database" % "h2" % "[1.4,)",
      "postgresql" % "postgresql" % "9.1-901.jdbc4",
      "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
        excludeAll ExclusionRule(organization = "org.specs2"),
      "org.scalikejdbc" %% "scalikejdbc" % "3.2.1" % "test",
      "org.scalikejdbc" %% "scalikejdbc-config" % "3.2.1" % "test",
      scalatest
    )
  )
  .dependsOn(plugin)
  .aggregate(plugin)

val publishingSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
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
  parallelExecution in Test := false
)
