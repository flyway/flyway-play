addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.16")

addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.5.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.13")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
