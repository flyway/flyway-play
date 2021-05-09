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

import play.api._

class ConfigReader(configuration: Configuration, environment: Environment) {

  case class JdbcConfig(driver: String, url: String, username: String, password: String)

  val urlParser = new UrlParser(environment: Environment)

  val logger = Logger(classOf[ConfigReader])

  private def dbConfigPrefix: String =
    configuration.getOptional[String]("flyway-config").getOrElse("db")

  private def getAllDatabaseNames: Seq[String] = (for {
    config <- configuration.getOptional[Configuration](dbConfigPrefix).toList
    dbName <- config.subKeys
  } yield {
    dbName
  }).distinct

  def getFlywayConfigurations: Map[String, FlywayConfiguration] = {
    (for {
      dbName <- getAllDatabaseNames
      database <- getDatabaseConfiguration(configuration, dbName)
      subConfig = configuration.getOptional[Configuration](s"$dbConfigPrefix.$dbName.migration").getOrElse(Configuration.empty)
    } yield {
      val placeholders = {
        subConfig.getOptional[Configuration]("placeholders").map { config =>
          config.subKeys.map { key => key -> config.getOptional[String](key).getOrElse("") }.toMap
        }.getOrElse(Map.empty)
      }

      dbName -> FlywayConfiguration(
        database,
        subConfig.getOptional[Boolean]("validateOnStart").getOrElse(false),
        subConfig.getOptional[Boolean]("auto").getOrElse(false),
        subConfig.getOptional[Seq[String]]("locations").getOrElse(Seq.empty[String]),
        subConfig.getOptional[String]("encoding"),
        subConfig.getOptional[Seq[String]]("schemas").getOrElse(Seq.empty[String]),
        subConfig.getOptional[String]("table"),
        subConfig.getOptional[Boolean]("placeholderReplacement"),
        placeholders,
        subConfig.getOptional[String]("placeholderPrefix"),
        subConfig.getOptional[String]("placeholderSuffix"),
        subConfig.getOptional[String]("sqlMigrationPrefix"),
        subConfig.getOptional[String]("repeatableSqlMigrationPrefix"),
        subConfig.getOptional[String]("sqlMigrationSeparator"),
        subConfig.getOptional[String]("sqlMigrationSuffix"),
        subConfig.getOptional[Seq[String]]("sqlMigrationSuffixes").getOrElse(Seq.empty[String]),
        subConfig.getOptional[Boolean]("ignoreFutureMigrations"),
        subConfig.getOptional[Boolean]("ignoreMissingMigrations"),
        subConfig.getOptional[Boolean]("validateOnMigrate"),
        subConfig.getOptional[Boolean]("cleanOnValidationError"),
        subConfig.getOptional[Boolean]("cleanDisabled"),
        subConfig.getOptional[Boolean]("initOnMigrate"),
        subConfig.getOptional[Boolean]("outOfOrder"),
        subConfig.getOptional[String]("scriptsDirectory"),
        subConfig.getOptional[Boolean]("mixed"),
        subConfig.getOptional[Boolean]("group"))
    }).toMap
  }

  private def getDatabaseConfiguration(configuration: Configuration, dbName: String): Option[DatabaseConfiguration] = {
    val jdbcConfigOrError = for {
      jdbcUrl <- configuration.getOptional[String](s"$dbConfigPrefix.$dbName.url").toRight(s"$dbConfigPrefix.$dbName.url is not set").right
      driver <- configuration.getOptional[String](s"$dbConfigPrefix.$dbName.driver").toRight(s"$dbConfigPrefix.$dbName.driver is not set").right
    } yield {
      val (parsedUrl, parsedUser, parsedPass) = urlParser.parseUrl(jdbcUrl)
      val username = parsedUser
        .orElse(configuration.getOptional[String](s"$dbConfigPrefix.$dbName.username"))
        .orElse(configuration.getOptional[String](s"$dbConfigPrefix.$dbName.user"))
        .orNull
      val password = parsedPass
        .orElse(configuration.getOptional[String](s"$dbConfigPrefix.$dbName.password"))
        .orElse(configuration.getOptional[String](s"$dbConfigPrefix.$dbName.pass"))
        .orNull
      JdbcConfig(driver, parsedUrl, username, password)
    }

    jdbcConfigOrError match {
      case Left(message) =>
        logger.warn(message)
        None
      case Right(jdbc) =>
        Some(DatabaseConfiguration(
          jdbc.driver,
          jdbc.url,
          jdbc.username,
          jdbc.password))
    }
  }

}
