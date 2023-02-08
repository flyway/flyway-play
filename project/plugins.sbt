addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.19")

addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.5.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.17")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
