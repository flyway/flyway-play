def _publishTo(v: String) = {
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

name := "flyway-play"

organization := "org.flywaydb"

version := "2.0.0-SNAPSHOT"

scalaVersion := "2.10.4"

crossScalaVersions := Seq(scalaVersion.value, "2.11.6")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % play.core.PlayVersion.current % "provided",
  "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
    excludeAll(ExclusionRule(organization = "org.specs2")),
  "org.flywaydb" % "flyway-core" % "3.1",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test"
)

scalacOptions ++= Seq("-language:_", "-deprecation")

publishMavenStyle := true

publishTo <<= version { (v: String) => _publishTo(v) }

publishArtifact in Test := false

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
      <url>http://tototoshi.github.com</url>
    </developer>
  </developers>

scalariformSettings
