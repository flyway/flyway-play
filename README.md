# play-flyway

Flyway plugin for Play 2.1 or later. It aims to be a substitute for play-evolutions.

## Features

 - Based on [Flyway](http://flywaydb.org/)
 - No 'Downs' part.
 - Independent of DBPlugin(play.api.db).

## Install

### For Play 2.3.x
In Build.scala/build.sbt

```scala
libraryDependencies += "com.github.tototoshi" %% "play-flyway" % "1.1.2"
```

and write play.plugins.

```
1000:com.github.tototoshi.play2.flyway.Plugin
```

### For Play 2.2.x
In Build.scala/build.sbt

```scala
libraryDependencies += "com.github.tototoshi" %% "play-flyway" % "1.0.4"
```

and write play.plugins.

```
1000:com.github.tototoshi.play2.flyway.Plugin
```

### For Play 2.1.x
In Build.scala/build.sbt

```scala
libraryDependencies += "com.github.tototoshi" %% "play-flyway" % "0.2.0"
```

and write play.plugins.

```
1000:com.github.tototoshi.play2.flyway.Plugin
```

## Document

Please see [Document site](http://tototoshi.github.io/play-flyway/)
