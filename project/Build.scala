import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "flyway_test"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "org.slf4j" % "slf4j-simple" % "1.7.2",
    "com.github.seratch" %% "scalikejdbc" % "[1.4,)",
    "com.googlecode.flyway" % "flyway-core" % "2.1.1",
    "commons-io" % "commons-io" % "2.4",
    "postgresql" % "postgresql" % "9.1-901.jdbc4"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  )

}
