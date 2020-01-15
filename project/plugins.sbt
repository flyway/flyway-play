// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.4.2")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
