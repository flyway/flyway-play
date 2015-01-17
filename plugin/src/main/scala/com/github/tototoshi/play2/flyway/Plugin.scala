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
package com.github.tototoshi.play2.flyway

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import play.core._
import java.io.FileNotFoundException
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import scala.collection.JavaConverters._

class Plugin(implicit app: Application) extends play.api.Plugin
    with HandleWebCommandSupport
    with PluginConfiguration
    with FileUtils {

  private val configReader = new ConfigReader(app)

  private val allDatabaseNames = configReader.getDatabaseConfigurations.keys

  private val flywayPrefixToMigrationScript = "db/migration"

  private def initOnMigrate(dbName: String): Boolean =
    app.configuration.getBoolean(s"db.${dbName}.migration.initOnMigrate").getOrElse(false)

  private def validateOnMigrate(dbName: String): Boolean =
    app.configuration.getBoolean(s"db.${dbName}.migration.validateOnMigrate").getOrElse(true)

  private def placeholderPrefix(dbName: String): Option[String] =
    app.configuration.getString(s"db.${dbName}.migration.placeholderPrefix")

  private def placeholderSuffix(dbName: String): Option[String] =
    app.configuration.getString(s"db.${dbName}.migration.placeholderSuffix")

  private def placeholders(dbName: String): Map[String, String] = {
    app.configuration.getConfig(s"db.${dbName}.migration.placeholders").map { config =>
      config.subKeys.map { key => (key -> config.getString(key).getOrElse("")) }.toMap
    }.getOrElse(Map.empty)
  }

  private def encoding(dbName: String): String = {
    app.configuration.getString(s"db.${dbName}.migration.encoding").getOrElse("UTF-8")
  }

  private def migrationFileDirectoryExists(path: String): Boolean = {
    app.resource(path) match {
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

  private lazy val flyways: Map[String, Flyway] = {
    for {
      (dbName, configuration) <- configReader.getDatabaseConfigurations
      migrationFilesLocation = s"db/migration/${dbName}"
      if migrationFileDirectoryExists(migrationFilesLocation)
    } yield {
      val flyway = new Flyway
      flyway.setDataSource(new DriverDataSource(getClass.getClassLoader, configuration.driver, configuration.url, configuration.user, configuration.password))
      flyway.setLocations(migrationFilesLocation)
      flyway.setValidateOnMigrate(validateOnMigrate(dbName))
      flyway.setEncoding(encoding(dbName))
      if (initOnMigrate(dbName)) {
        flyway.setBaselineOnMigrate(true)
      }
      for (prefix <- placeholderPrefix(dbName)) {
        flyway.setPlaceholderPrefix(prefix)
      }
      for (suffix <- placeholderSuffix(dbName)) {
        flyway.setPlaceholderSuffix(suffix)
      }
      flyway.setPlaceholders(placeholders(dbName).asJava)

      dbName -> flyway
    }
  }

  override lazy val enabled: Boolean =
    !app.configuration.getString("flywayplugin").exists(_ == "disabled")

  private def migrationDescriptionToShow(dbName: String, migration: MigrationInfo): String = {
    app.resourceAsStream(s"${flywayPrefixToMigrationScript}/${dbName}/${migration.getScript}").map { in =>
      s"""|--- ${migration.getScript} ---
          |${readInputStreamToString(in)}""".stripMargin
    }.orElse {
      import scala.util.control.Exception._
      allCatch opt { Class.forName(migration.getScript) } map { cls =>
        s"""|--- ${migration.getScript} ---
            | (Java-based migration)""".stripMargin
      }
    }.getOrElse(throw new FileNotFoundException(s"Migration file not found. ${migration.getScript}"))
  }

  private def checkState(dbName: String): Unit = {
    flyways.get(dbName).foreach { flyway =>
      val pendingMigrations = flyway.info().pending
      if (!pendingMigrations.isEmpty) {
        throw InvalidDatabaseRevision(
          dbName,
          pendingMigrations.map(migration => migrationDescriptionToShow(dbName, migration)).mkString("\n"))
      }
    }
  }

  override def onStart(): Unit = {
    for (dbName <- allDatabaseNames) {
      if (Play.isTest || app.configuration.getBoolean(s"db.${dbName}.migration.auto").getOrElse(false)) {
        migrateAutomatically(dbName)
      } else {
        checkState(dbName)
      }
    }
  }

  override def onStop(): Unit = {
  }

  private def migrateAutomatically(dbName: String): Unit = {
    flyways.get(dbName).foreach { flyway =>
      flyway.migrate()
    }
  }

  override def handleWebCommand(request: RequestHeader, sbtLink: BuildLink, path: java.io.File): Option[SimpleResult] = {
    val webCommand = new FlywayWebCommand(app, flywayPrefixToMigrationScript, flyways)
    webCommand.handleWebCommand(request: RequestHeader, sbtLink: BuildLink, path: java.io.File)
  }

}
