addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.13")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.5.1")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.11")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
