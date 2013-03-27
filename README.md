# play-flyway

Flyway plugin for Play 2.x. It aims to be a substitute for play-evolutions.

## Install

Maybe I will deploy it to Maven central soon. For now, try publish-local.

and write play.plugins.

```
1000:com.github.tototoshi.play2.flyway.Plugin
```

## Usage

Almost the same as play-evolutions.

Place your migration scripts in config/db/migration/${dbName} .


![screenshot](/screenshot1.png)

