addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.4")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.11.0")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
