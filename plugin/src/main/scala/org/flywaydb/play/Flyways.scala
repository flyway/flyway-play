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
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.internal.jdbc.DriverDataSource
import play.api.{ Configuration, Environment, Logger }

import scala.collection.JavaConverters._

@Singleton
class Flyways @Inject() (
  configuration: Configuration,
  environment: Environment) {

  val flywayPrefixToMigrationScript: String = "db/migration"

  private val flywayConfigurations = {
    val configReader = new ConfigReader(configuration, environment)
    configReader.getFlywayConfigurations
  }

  val allDatabaseNames: Seq[String] = flywayConfigurations.keys.toSeq

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
      configuration.mixed.foreach(flyway.mixed)
      configuration.group.foreach(flyway.group)

      dbName -> flyway.load()
    }
  }

  def config(dbName: String): FlywayConfiguration =
    flywayConfigurations.getOrElse(dbName, sys.error(s"database $dbName not found"))

  def allMigrationInfo(dbName: String): Seq[MigrationInfo] =
    flyways.get(dbName).toSeq.flatMap(_.info().all())

  def schemaTable(dbName: String): String = {
    val flyway = flyways.getOrElse(dbName, sys.error(s"database $dbName not found"))
    flyway.getConfiguration.getTable
  }

  def migrate(dbName: String): Unit = {
    flyways.get(dbName).foreach(_.migrate())
  }

  def clean(dbName: String): Unit = {
    flyways.get(dbName).foreach(_.clean)
  }

  def repair(dbName: String): Unit = {
    flyways.get(dbName).foreach(_.repair)
  }

  def baseline(dbName: String, version: String): Unit = {
    flyways.get(dbName).foreach {
      flyway =>
        Flyway.configure()
          .configuration(flyway.getConfiguration)
          .baselineVersion(version)
          .load()
          .baseline()

    }
  }

  private def migrationFileDirectoryExists(path: String): Boolean = {
    environment.resource(path) match {
      case Some(_) =>
        Logger("flyway").debug(s"Directory for migration files found. $path")
        true

      case None =>
        Logger("flyway").warn(s"Directory for migration files not found. $path")
        false

    }
  }

  private def setSqlMigrationSuffixes(configuration: FlywayConfiguration, flyway: FluentConfiguration): Unit = {
    configuration.sqlMigrationSuffix.foreach(_ =>
      Logger("flyway").warn("sqlMigrationSuffix is deprecated in Flyway 5.0, and will be removed in a future version. Use sqlMigrationSuffixes instead."))
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
      allCatch opt {
        environment.classLoader.loadClass(migration.getScript)
      } map { _ =>
        s"""|--- ${migration.getScript} ---
            |$code""".stripMargin
      }
    }.getOrElse(throw new FileNotFoundException(s"Migration file not found. ${migration.getScript}"))
  }

  def checkState(dbName: String): Unit = {
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

}
