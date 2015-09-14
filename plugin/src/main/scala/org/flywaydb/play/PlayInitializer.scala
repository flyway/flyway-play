/*
 * Copyright 2013 Toshiyuki Takahashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.play

import java.io.FileNotFoundException
import javax.inject._

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import play.api._
import play.core._

import scala.collection.JavaConverters._

@Singleton
class PlayInitializer @Inject() (
    configuration: Configuration,
    environment: Environment,
    webCommands: WebCommands
) {

  private val flywayConfigurations = {
    val configReader = new ConfigReader(configuration, environment)
    configReader.getFlywayConfigurations
  }

  private val allDatabaseInfo = flywayConfigurations.keys

  private val flywayPrefixToMigrationScript = "migrations"

  private def migrationFileDirectoryExists(path: String): Boolean = {
    environment.resource(path) match {
      case Some(r) => {
        Logger.debug(s"Directory for migration files found. ${path}")
        true
      }
      case None => {
        Logger.warn(s"Directory for migration files not found. ${path}")
        false
      }
    }
  }

  private def getMigrationDirectoryPath(databaseInfo: DatabaseInfo): String = {
    databaseInfo match {
      case DefaultDatabaseInfo(dbName, confPath) => s"$flywayPrefixToMigrationScript/db/${dbName}"
      case SlickDatabaseInfo(slickDbName, slickConfPath) => s"$flywayPrefixToMigrationScript/slick/${slickDbName}"
    }
  }

  private lazy val flyways: Map[DatabaseInfo, Flyway] = {
    for {
      (dbInfo, configuration) <- flywayConfigurations
      migrationFilesLocation = getMigrationDirectoryPath(dbInfo)
      if migrationFileDirectoryExists(migrationFilesLocation)
    } yield {
      val flyway = new Flyway
      val database = configuration.database
      flyway.setDataSource(new DriverDataSource(getClass.getClassLoader, database.driver, database.url, database.user, database.password))
      flyway.setLocations(migrationFilesLocation)
      flyway.setValidateOnMigrate(configuration.validateOnMigrate)
      flyway.setEncoding(configuration.encoding)
      flyway.setOutOfOrder(configuration.outOfOrder)

      if (configuration.initOnMigrate) {
        flyway.setBaselineOnMigrate(true)
      }

      for (prefix <- configuration.placeholderPrefix) {
        flyway.setPlaceholderPrefix(prefix)
      }

      for (suffix <- configuration.placeholderSuffix) {
        flyway.setPlaceholderSuffix(suffix)
      }

      flyway.setSchemas(configuration.schemas: _*)
      flyway.setPlaceholders(configuration.placeholders.asJava)

      dbInfo -> flyway
    }
  }

  private def migrationDescriptionToShow(dbName: String, migration: MigrationInfo, dbType: String = "db"): String = {

    environment.resourceAsStream(s"${flywayPrefixToMigrationScript}/${dbType}/${dbName}/${migration.getScript}").map { in =>
      s"""|--- ${migration.getScript} ---
          |${FileUtils.readInputStreamToString(in)}""".stripMargin
    }.orElse {
      import scala.util.control.Exception._
      val code = for {
        script <- FileUtils.findJdbcMigrationFile(environment.rootPath, migration.getScript)
      } yield FileUtils.readFileToString(script)
      allCatch opt { environment.classLoader.loadClass(migration.getScript) } map { cls =>
        s"""|--- ${migration.getScript} ---
            |$code""".stripMargin
      }
    }.getOrElse(throw new FileNotFoundException(s"Migration file not found. ${migration.getScript}"))
  }

  private def checkState(dbInfo: DatabaseInfo): Unit = {
    flyways.get(dbInfo).foreach { flyway =>
      val pendingMigrations = flyway.info().pending
      if (!pendingMigrations.isEmpty) {
        val dbType = dbInfo match {
          case s: SlickDatabaseInfo => "slick"
          case _ => "db"
        }
        throw InvalidDatabaseRevision(
          dbInfo.name,
          dbType,
          pendingMigrations.map(migration => migrationDescriptionToShow(dbInfo.name, migration, dbType)).mkString("\n")
        )
      }
    }
  }

  def onStart(): Unit = {
    val flywayWebCommand = new FlywayWebCommand(configuration, environment, flywayPrefixToMigrationScript, flyways)
    webCommands.addHandler(flywayWebCommand)

    for (dbInfo <- allDatabaseInfo) {
      if (environment.mode == Mode.Test || flywayConfigurations(dbInfo).auto) {
        migrateAutomatically(dbInfo)
      } else {
        checkState(dbInfo)
      }
    }
  }

  private def migrateAutomatically(dbInfo: DatabaseInfo): Unit = {
    flyways.get(dbInfo).foreach { flyway =>
      flyway.migrate()
    }
  }

  val enabled: Boolean = !configuration.getString("flywayplugin").exists(_ == "disabled")

  if (enabled) {
    onStart()
  }
}
