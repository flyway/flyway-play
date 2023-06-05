addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.0-M6")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
