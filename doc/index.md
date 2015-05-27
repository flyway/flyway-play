## <a class="anchor" name="features"></a>Features

 - Based on [Flyway](http://flywaydb.org/)
 - No 'Downs' part.
 - Independent of DBPlugin(play.api.db).

## <a class="anchor" name="install"></a>Install

build.sbt

```scala
libraryDependencies ++= Seq(
  "org.flywaydb" %% "flyway-play" % "2.0.0-RC1"
)
```

conf/application.conf

```
play.modules.enabled += "org.flywaydb.play.PlayModule"
```


## <a class="anchor" name="getting-started"></a>Getting Started


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

### Placeholders

Flyway can replace placeholders in Sql migrations.
The default pattern is ${placeholder}.
This can be configured using the placeholderPrefix and placeholderSuffix properties.

The placeholder prefix, suffix and key-value pairs can be specificed in application.conf, e.g.

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

### Dev

![screenshot](images/screenshot1.png)


For existing schema, Flyway has a option called 'initOnMigrate'. This option is enabled when `-Ddb.${dbName}.migration.initOnMigrate=true`.
For example,
```
$ play -Ddb.default.migration.initOnMigrate=true
```

Of course, You can write this in your `application.conf`.


Manual migration is also supported. Click 'Other operations' or open `/@flyway/${dbName}` directly.

![screenshot](images/screenshot2.png)


### Test

In Test mode, migration is done automatically.


### Prod

In production mode, migration is done automatically if `db.${dbName}.migration.auto` is set to be true in application.conf.
Otherwise it failed to start when migration is needed.

```
$ play -Ddb.default.migration.auto=true start
```

## <a class="anchor" name="example"></a>Example application

[seratch/devteam-app](https://github.com/seratch/devteam-app "seratch/devteam-app") is using play-flyway. Maybe this is a good example.

## <a class="anchor" name="changelog"></a>Change Log

## <a class="anchor" name="license"></a>License

- Apache 2.0 License
