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

import javax.inject.{ Inject, Singleton }
import org.flywaydb.core.api.MigrationInfo
import play.core._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.internal.jdbc.DriverDataSource

import scala.collection.JavaConverters._

@Singleton
class FlywayWebCommand @Inject() (
  configuration: Configuration,
  environment: Environment)
  extends HandleWebCommandSupport {

  private var checkedAlready = false

  private val flywayPrefixToMigrationScript = "db/migration"

  private val flywayConfigurations = {
    val configReader = new ConfigReader(configuration, environment)
    configReader.getFlywayConfigurations
  }

  private val allDatabaseNames = flywayConfigurations.keys

  private lazy val flyways: Map[String, Flyway] = {
    for {
      (dbName, configuration) <- flywayConfigurations
      migrationFilesLocation = s"$flywayPrefixToMigrationScript/${configuration.scriptsDirectory.getOrElse(dbName)}"
      if migrationFileDirectoryExists(migrationFilesLocation)
    } yield {
      val flyway = Flyway.configure(environment.classLoader)
      val database = configuration.database
      val dataSource = new DriverDataSource(
        getClass.getClassLoader,
        database.driver,
        database.url,
        database.user,
        database.password,
        null)
      flyway.dataSource(dataSource)
      if (configuration.locations.nonEmpty) {
        val locations = configuration.locations.map(location => s"$migrationFilesLocation/$location")
        flyway.locations(locations: _*)
      } else {
        flyway.locations(migrationFilesLocation)
      }
      configuration.encoding.foreach(flyway.encoding)
      flyway.schemas(configuration.schemas: _*)
      configuration.table.foreach(flyway.table)
      configuration.placeholderReplacement.foreach(flyway.placeholderReplacement)
      flyway.placeholders(configuration.placeholders.asJava)
      configuration.placeholderPrefix.foreach(flyway.placeholderPrefix)
      configuration.placeholderSuffix.foreach(flyway.placeholderSuffix)
      configuration.sqlMigrationPrefix.foreach(flyway.sqlMigrationPrefix)
      configuration.repeatableSqlMigrationPrefix.foreach(flyway.repeatableSqlMigrationPrefix)
      configuration.sqlMigrationSeparator.foreach(flyway.sqlMigrationSeparator)
      setSqlMigrationSuffixes(configuration, flyway)
      configuration.ignoreFutureMigrations.foreach(flyway.ignoreFutureMigrations)
      configuration.ignoreMissingMigrations.foreach(flyway.ignoreMissingMigrations)
      configuration.validateOnMigrate.foreach(flyway.validateOnMigrate)
      configuration.cleanOnValidationError.foreach(flyway.cleanOnValidationError)
      configuration.cleanDisabled.foreach(flyway.cleanDisabled)
      configuration.initOnMigrate.foreach(flyway.baselineOnMigrate)
      configuration.outOfOrder.foreach(flyway.outOfOrder)

      dbName -> flyway.load()
    }
  }

  private def migrationFileDirectoryExists(path: String): Boolean = {
    environment.resource(path) match {
      case Some(_) =>
        Logger.debug(s"Directory for migration files found. $path")
        true

      case None =>
        Logger.warn(s"Directory for migration files not found. $path")
        false

    }
  }

  private def setSqlMigrationSuffixes(configuration: FlywayConfiguration, flyway: FluentConfiguration): Unit = {
    configuration.sqlMigrationSuffix.foreach(_ =>
      Logger.warn("sqlMigrationSuffix is deprecated in Flyway 5.0, and will be removed in a future version. Use sqlMigrationSuffixes instead."))
    val suffixes: Seq[String] = configuration.sqlMigrationSuffixes ++ configuration.sqlMigrationSuffix
    if (suffixes.nonEmpty) flyway.sqlMigrationSuffixes(suffixes: _*)
  }

  private def migrationDescriptionToShow(dbName: String, migration: MigrationInfo): String = {
    val locations = flywayConfigurations(dbName).locations
    (if (locations.nonEmpty) locations.map(location => environment.resourceAsStream(s"$flywayPrefixToMigrationScript/${flywayConfigurations(dbName).scriptsDirectory.getOrElse(dbName)}/$location/${migration.getScript}"))
      .find(resource => resource.nonEmpty).flatten
    else {
      environment.resourceAsStream(s"$flywayPrefixToMigrationScript/${flywayConfigurations(dbName).scriptsDirectory.getOrElse(dbName)}/${migration.getScript}")
    }).map { in =>
      s"""|--- ${migration.getScript} ---
          |${FileUtils.readInputStreamToString(in)}""".stripMargin
    }.orElse {
      import scala.util.control.Exception._
      val code = for {
        script <- FileUtils.findJdbcMigrationFile(environment.rootPath, migration.getScript)
      } yield FileUtils.readFileToString(script)
      allCatch opt { environment.classLoader.loadClass(migration.getScript) } map { _ =>
        s"""|--- ${migration.getScript} ---
            |$code""".stripMargin
      }
    }.getOrElse(throw new FileNotFoundException(s"Migration file not found. ${migration.getScript}"))
  }

  private def checkState(dbName: String): Unit = {
    flyways.get(dbName).foreach { flyway =>
      val pendingMigrations = flyway.info().pending
      if (pendingMigrations.nonEmpty) {
        throw InvalidDatabaseRevision(
          dbName,
          pendingMigrations.map(migration => migrationDescriptionToShow(dbName, migration)).mkString("\n"))
      }

      if (flywayConfigurations(dbName).validateOnStart) {
        flyway.validate()
      }
    }
  }

  private def migrateAutomatically(dbName: String): Unit = {
    flyways.get(dbName).foreach { flyway =>
      flyway.migrate()
    }
  }

  def handleWebCommand(request: RequestHeader, sbtLink: BuildLink, path: java.io.File): Option[Result] = {

    def withDatabase(dbName: String)(f: Flyway => Result): Result = {
      flyways.get(dbName).map(f).getOrElse(NotFound(s"database $dbName not found"))
    }

    request.path match {
      case WebCommandPath.migratePath(dbName) =>
        val result = withDatabase(dbName) { flyway =>
          flyway.migrate()
          sbtLink.forceReload()
          Redirect(getRedirectUrlFromRequest(request))
        }
        Some(result)
      case WebCommandPath.cleanPath(dbName) =>
        val result = withDatabase(dbName) { flyway =>
          flyway.clean()
          Redirect(getRedirectUrlFromRequest(request))
        }
        Some(result)
      case WebCommandPath.repairPath(dbName) =>
        val result = withDatabase(dbName) { flyway =>
          flyway.repair()
          Redirect(getRedirectUrlFromRequest(request))
        }
        Some(result)
      case WebCommandPath.versionedInitPath(dbName, version) =>
        val result = withDatabase(dbName) { flyway =>
          Flyway.configure()
            .configuration(flyway.getConfiguration)
            .baselineVersion(version)
            .load()
            .baseline()
          Redirect(getRedirectUrlFromRequest(request))
        }
        Some(result)
      case WebCommandPath.showInfoPath(dbName) =>
        val result = withDatabase(dbName) { flyway =>
          val allMigrationInfo: Seq[MigrationInfo] = flyways.get(dbName).toSeq.flatMap(_.info().all())
          val scriptsDirectory = configuration.getOptional[String](s"db.$dbName.migration.scriptsDirectory").getOrElse(dbName)
          val scripts: Seq[String] = allMigrationInfo.map { info =>
            environment.resourceAsStream(s"$flywayPrefixToMigrationScript/$scriptsDirectory/${info.getScript}").map { in =>
              FileUtils.readInputStreamToString(in)
            }.orElse {
              for {
                script <- FileUtils.findJdbcMigrationFile(environment.rootPath, info.getScript)
              } yield FileUtils.readFileToString(script)
            }.getOrElse("")
          }
          val showManualInsertQuery = configuration.getOptional[Boolean](s"db.$dbName.migration.showInsertQuery").getOrElse(false)
          val schemaTable = flyway.getConfiguration.getTable
          Ok(views.html.info(request, dbName, allMigrationInfo, scripts, showManualInsertQuery, schemaTable)).as("text/html")
        }
        Some(result)
      case "/@flyway" =>
        Some(Ok(views.html.index(flyways.keys.toSeq)).as("text/html"))
      case _ =>
        if (!checkedAlready) {
          for (dbName <- allDatabaseNames) {
            if (environment.mode == Mode.Test || flywayConfigurations(dbName).auto) {
              migrateAutomatically(dbName)
            } else {
              checkState(dbName)
            }
          }
          checkedAlready = true
        }
        None
    }
  }

  private def getRedirectUrlFromRequest(request: RequestHeader): String = {
    (for {
      urls <- request.queryString.get("redirect")
      url <- urls.headOption
    } yield url).getOrElse("/")
  }

}
