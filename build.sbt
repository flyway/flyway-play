lazy val plugin = project

lazy val playapp = project.dependsOn(plugin).aggregate(plugin).enablePlugins(PlayScala)

lazy val root = project.in(file(".")).aggregate(plugin, playapp)