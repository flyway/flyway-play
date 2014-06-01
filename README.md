# play-flyway

Flyway plugin for Play 2.1. It aims to be a substitute for play-evolutions.

## Features

 - Based on [Flyway](http://flywaydb.org/)
 - No 'Downs' part.
 - Independent of DBPlugin(play.api.db).

## Install

### For Play 2.3.x
In Build.scala/build.sbt

```scala
libraryDependencies += "com.github.tototoshi" %% "play-flyway" % "1.1.0"
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

## Usage


### Sample

[seratch/devteam-app](https://github.com/seratch/devteam-app "seratch/devteam-app") is using play-flyway. Maybe this is a good example.

### Place migration scripts

A migration script is just a simple SQL file.

```sql
CREATE TABLE FOO (.............


```

Place your migration scripts in `conf/db/migration/${dbName}` .

```
playapp
├── app
│   ├── controllers
│   ├── models
│   └── views
├── conf
│   ├── application.conf
│   ├── db
│   │   └── migration
│   │       ├── default
│   │       │   ├── V1__Create_person_table.sql
│   │       │   └── V2__Add_people.sql
│   │       └── secondary
│   │           ├── V1__create_job_table.sql
│   │           └── V2__Add_job.sql
│   ├── play.plugins
│   └── routes
```


Please see flyway's documents about the naming convention for migration scripts.

http://flywaydb.org/documentation/migration/sql.html


### Dev


![screenshot](/screenshot1.png)


For existing schema, Flyway has a option called 'initOnMigrate'. This option is enabled when `-Ddb.${dbName}.migration`.initOnMigrate=true.
For example,
```
$ play -Ddb.default.migration.initOnMigrate=true
```

Of course, You can write this in your `application.conf`.


Manual migration is also supported. Click 'Other operations' or open `/@flyway/${dbName}` directly.

![screenshot](/screenshot2.png)


### Test

In Test mode, migration is done automatically.


### Prod

In production mode, migration is done automatically if `db.${dbName}.migration.auto` is set to be true in application.conf.
Otherwise it failed to start when migration is needed.

```
$ play -Ddb.default.migration.auto=true start
```


## Change Log

### 1.1.0
- Suport Play 2.3.0 and Flyway 3.0
- Applied Twitter Bootstrap theme
- Delay initialization (#17)
- Init with version number (#8)

### 1.0.4
- Add URL parser for URLs starting with "postgres://" etc. (#11)

### 1.0.3
- Disable the plugin when `flywayplugin=disabled` is found in application.conf.

### 1.0.2
- Fixed #7: Reading "db.${dbName}.password", but not "db.${dbName}.pass"

### 1.0.1
- Supports reading the driver configuration parameter from Play2 config.
- Updated flyway to 2.3.

