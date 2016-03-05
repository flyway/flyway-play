// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
