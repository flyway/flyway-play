# flyway-play

![travis](https://api.travis-ci.org/flyway/flyway-play.svg)

Flyway module for Play 2.4 or later. It aims to be a substitute for play-evolutions.

This module is successor of [tototoshi/play-flyway](https://github.com/tototoshi/play-flyway), which is a Play Plugin supporting Play 2.1 ~ 2.3.


## Install

build.sbt

```scala
resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.flywaydb" %% "flyway-play" % "2.0.0-SNAPSHOT"
)
```

conf/application.conf

```
play.modules.enabled += "org.flywaydb.play.PlayModule"
```
