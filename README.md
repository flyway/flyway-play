# flyway-play

![Scala CI](https://github.com/flyway/flyway-play/workflows/Scala%20CI/badge.svg)

Flyway module for Play 2.4 or later. It aims to be a substitute for play-evolutions.

## <a class="anchor" name="features"></a>Features

- Based on [Flyway](https://flywaydb.org/)
- No 'Downs' part.
- Independent of DBPlugin(play.api.db).

## <a class="anchor" name="install"></a>Install

| flyway-play version | play version | flyway version |
| ------------------- | ------------ | -------------- |
| 7.27.0              | 2.8.x        | 9.5.0          |
| 7.26.0              | 2.8.x        | 9.4.0          |
| 7.25.0              | 2.8.x        | 9.3.0          |
| 7.24.0              | 2.8.x        | 9.2.0          |
| 7.23.0              | 2.8.x        | 9.1.0          |
| 7.22.0              | 2.8.x        | 9.0.0          |
| 7.21.0              | 2.8.x        | 8.5.0          |
| 7.20.0              | 2.8.x        | 8.5.0          |
| 7.19.0              | 2.8.x        | 8.4.0          |
| 7.18.0              | 2.8.x        | 8.3.0          |
| 7.17.0              | 2.8.x        | 8.2.0          |
| 7.16.0              | 2.8.x        | 8.1.0          |
| 7.15.0              | 2.8.x        | 8.0.0          |
| 7.14.0              | 2.8.x        | 7.14.0         |
| 7.13.0              | 2.8.x        | 7.13.0         |
| 7.12.0              | 2.8.x        | 7.12.0         |
| 7.11.0              | 2.8.x        | 7.11.0         |
| 7.10.0              | 2.8.x        | 7.10.0         |
| 7.9.0               | 2.8.x        | 7.9.0          |
| 7.8.0               | 2.8.x        | 7.8.0          |
| 7.7.0               | 2.8.x        | 7.7.0          |
| 7.6.0               | 2.8.x        | 7.6.0          |
| 7.5.0               | 2.8.x        | 7.5.4          |
| 7.2.0               | 2.8.x        | 7.2.1          |
| 6.5.0               | 2.8.x        | 6.5.7          |
| 6.2.0               | 2.8.x        | 6.2.4          |
| 6.0.0               | 2.8.x        | 6.1.0          |
| 5.4.0               | 2.7.x        | 6.0.1          |
| 5.3.2               | 2.7.x        | 5.2.4          |
| 5.2.0               | 2.6.x        | 5.2.4          |
| 5.1.0               | 2.6.x        | 5.1.4          |
| 5.0.0               | 2.6.x        | 5.0.7          |
| 4.0.0               | 2.6.x        | 4.2.0          |
| 3.2.0               | 2.5.x        | 4.2.0          |

build.sbt

```scala
libraryDependencies ++= Seq(
  "org.flywaydb" %% "flyway-play" % "7.27.0"
)
```

conf/application.conf

```
play.modules.enabled += "org.flywaydb.play.PlayModule"
```

## Maintenance

This repository is a community project and not officially maintained by the Flyway Team at Redgate.
This project is looked after only by the open source community. Community Maintainers are people who have agreed to be contacted with queries for support and maintenance.
Community Maintainers:

- [@tototoshi](https://github.com/tototoshi)

If you would like to be named as a Community Maintainer, let us know via Twitter: https://twitter.com/flywaydb.

## <a class="anchor" name="getting-started"></a>Getting Started

### Basic configuration

Database settings can be set in the manner of Play2.

```
db.default.driver=org.h2.Driver
db.default.url="jdbc:h2:mem:example2;db_CLOSE_DELAY=-1"
db.default.username="sa"
db.default.password="secret"

# optional
db.default.migration.schemas=["public", "other"]
```

### Place migration scripts

A migration script is just a simple SQL file.

```sql
CREATE TABLE FOO (.............


```

By default place your migration scripts in `conf/db/migration/${dbName}` .

If scriptsDirectory parameter is set, it will look for migrations scripts in `conf/db/migration/${scriptsDirectory}` .

```
playapp
├── app
│   ├── controllers
│   ├── models
│   └── views
├── conf
│   ├── application.conf
│   ├── db
│   │   └── migration
│   │       ├── default
│   │       │   ├── V1__Create_person_table.sql
│   │       │   └── V2__Add_people.sql
│   │       └── secondary
│   │           ├── V1__create_job_table.sql
│   │           └── V2__Add_job.sql
│   ├── play.plugins
│   └── routes
```

Alternatively, specify one or more locations per database and place your migrations in `conf/db/migration/${dbName}/${locations[1...N]}` . By varying the locations in each environment you are able to specify different scripts per RDBMS for each upgrade. These differences should be kept minimal.

For example, in testing use the configuration:

```
db.default.migration.locations=["common","h2"]
```

And in production use the configuration:

```
db.default.migration.locations=["common","mysql"]
```

Then put your migrations in these folders. Note that the migrations for the `secondary` database remain in the default location.

```
playapp
├── app
│   ├── controllers
│   ├── models
│   └── views
├── conf
│   ├── application.conf
│   ├── db
│   │   └── migration
│   │       ├── default
│   │       │   ├── common
│   │       │   │   └── V2__Add_people.sql
│   │       │   ├── h2
│   │       │   │   └── V1__Create_person_table.sql
│   │       │   └── mysql
│   │       │       └── V1__Create_person_table.sql
│   │       └── secondary
│   │           ├── V1__create_job_table.sql
│   │           └── V2__Add_job.sql
│   ├── play.plugins
│   └── routes
```

Please see flyway's documents about the naming convention for migration scripts.

https://flywaydb.org/documentation/migration/sql.html

### Placeholders

Flyway can replace placeholders in Sql migrations.
The default pattern is ${placeholder}.
This can be configured using the placeholderPrefix and placeholderSuffix properties.

The placeholder prefix, suffix, and key-value pairs can be specified in application.conf, e.g.

```
db.default.migration.placeholderPrefix="$flyway{{{"
db.default.migration.placeholderSuffix="}}}"
db.default.migration.placeholders.foo="bar"
db.default.migration.placeholders.hoge="pupi"
```

This would cause

```sql
INSERT INTO USERS ($flyway{{{foo}}}) VALUES ('$flyway{{{hoge}}}')
```

to be rewritten to

```sql
INSERT INTO USERS (bar) VALUES ('pupi')
```

### Enable/disable Validation

From flyway 3.0, `validate` run before `migrate` by default.
Set `validateOnMigrate` to false if you want to disable this.

```
db.${dbName}.migration.validateOnMigrate=false // true by default
```

### Migration prefix

Custom sql migration prefix key-value pair can be specified in application.conf:

```
db.${dbName}.migration.sqlMigrationPrefix="migration_"
```

### Insert Query

If you want to apply your migration not via the web interface, but manually on
your production databases you also need the valid insert query for flyway.

```
db.${dbName}.migration.showInsertQuery=true
```

### Dev

![screenshot](screenshot1.png)

For existing schema, Flyway has a option called 'initOnMigrate'. This option is enabled when `-Ddb.${dbName}.migration.initOnMigrate=true`.
For example,

```
$ play -Ddb.default.migration.initOnMigrate=true
```

Of course, You can write this in your `application.conf`.

Manual migration is also supported. Click 'Other operations' or open `/@flyway/${dbName}` directly.

![screenshot](screenshot2.png)

### Test

In Test mode, migration is done automatically.

### Prod

In production mode, migration is done automatically if `db.${dbName}.migration.auto` is set to be true in application.conf.
Otherwise, it failed to start when migration is needed.

```
$ play -Ddb.default.migration.auto=true start
```

## <a class="anchor" name="example"></a>Example application

[seratch/devteam-app](https://github.com/scalikejdbc/devteam-app "seratch/devteam-app") is using play-flyway. Maybe this is a good example.

## compile-time DI support

```scala
class MyComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with FlywayPlayComponents
    ...
    {
  flywayPlayInitializer
  ...
}
```

## <a class="anchor" name="license"></a>License

- Apache 2.0 License
