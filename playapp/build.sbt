resourceDirectories in Test += baseDirectory.value / "conf"

scalaVersion := "2.10.4"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "[1.3,)",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
    excludeAll (ExclusionRule(organization = "org.specs2")),
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.4.0-M2-20141215",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test"
)
