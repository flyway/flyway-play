import sbt.Keys.resolvers

val scalaVersion_2_11 = "2.11.12"
val scalaVersion_2_12 = "2.12.10"
val scalaVersion_2_13 = "2.13.0"

val flywayVersion = "6.0.1"
val flywayPlayVersion = "5.4.101"
val scalikejdbcVersion = "3.3.5"

val scalatest = "org.scalatest" %% "scalatest" % "3.0.8" % "test"

lazy val sArtSettings = Seq(
  organization := "com.sa",
  resolvers += "Nexus Releases" at "https://nexus.s-art.co.nz/repository/maven-releases",
  resolvers += "Nexus Snapshots" at "https://nexus.s-art.co.nz/repository/maven-snapshots",
  publishTo := {
    val nexus = "https://nexus.s-art.co.nz/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "repository/maven-snapshots")
    else
      Some("releases" at nexus + "repository/maven-releases")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials.sa")
)

lazy val fpSettings = Seq(
  organization := "com.mattgilbert",
  resolvers += "Nexus Releases" at "https://nexus.financialplatforms.co.nz/nexus/content/repositories/releases",
  resolvers += "Nexus Snapshots" at "https://nexus.financialplatforms.co.nz/nexus/content/repositories/snapshots",
  publishTo := {
    val nexus = "https://nexus.financialplatforms.co.nz/nexus/content/repositories/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "snapshots")
    else
      Some("releases" at nexus + "releases")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
)

lazy val commonSettings = Seq(
  organization := "com.sa",
  scalaVersion := scalaVersion_2_12,
  // crossScalaVersions := Seq(scalaVersion_2_11, scalaVersion_2_12, scalaVersion_2_13),
) ++ sArtSettings

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
      resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
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
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion % "test",
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion % "test",
      scalatest
    )
  )
  .dependsOn(plugin)
  .aggregate(plugin)

val publishingSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishArtifact in(Compile, packageDoc) := false,
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
