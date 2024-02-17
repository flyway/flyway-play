addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.1")

addSbtPlugin("org.playframework.twirl" % "sbt-twirl" % "2.0.3")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
