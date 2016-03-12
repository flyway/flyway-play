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
import scala.collection.JavaConverters._

class ConfigReader(configuration: Configuration, environment: Environment) {

  case class JdbcConfig(driver: String, url: String, username: String, password: String)

  val urlParser = new UrlParser(environment: Environment)

  val logger = Logger(classOf[ConfigReader])

  private def getAllDatabaseNames: Seq[String] = (for {
    config <- configuration.getConfig("db").toList
    dbName <- config.subKeys
  } yield {
    dbName
  }).distinct

  def getFlywayConfigurations: Map[String, FlywayConfiguration] = {
    (for {
      dbName <- getAllDatabaseNames
      jdbc <- getJdbcConfig(configuration, dbName)
      subConfig = configuration.getConfig(s"db.$dbName.migration").getOrElse(Configuration.empty)
    } yield {
      val initOnMigrate = subConfig.getBoolean("initOnMigrate").getOrElse(false)
      val validateOnMigrate = subConfig.getBoolean("validateOnMigrate").getOrElse(true)
      val encoding = subConfig.getString("encoding").getOrElse("UTF-8")
      val placeholderPrefix = subConfig.getString("placeholderPrefix")
      val placeholderSuffix = subConfig.getString("placeholderSuffix")

      val placeholders = {
        subConfig.getConfig("placeholders").map { config =>
          config.subKeys.map { key => (key -> config.getString(key).getOrElse("")) }.toMap
        }.getOrElse(Map.empty)
      }

      val outOfOrder = subConfig.getBoolean("outOfOrder").getOrElse(false)
      val auto = subConfig.getBoolean("auto").getOrElse(false)
      val schemas = subConfig.getStringList("schemas").getOrElse(java.util.Collections.emptyList[String]).asScala.toList
      val locations = subConfig.getStringList("locations").getOrElse(java.util.Collections.emptyList[String]).asScala.toList
      val sqlMigrationPrefix = subConfig.getString("sqlMigrationPrefix")

      val database = DatabaseConfiguration(
        jdbc.driver,
        jdbc.url,
        jdbc.username,
        jdbc.password)

      dbName -> FlywayConfiguration(
        database,
        auto,
        initOnMigrate,
        validateOnMigrate,
        encoding,
        placeholderPrefix,
        placeholderSuffix,
        placeholders,
        outOfOrder,
        schemas,
        locations,
        sqlMigrationPrefix
      )
    }).toMap
  }

  private def getJdbcConfig(configuration: Configuration, dbName: String): Option[JdbcConfig] = {
    val jdbcConfigOrError = for {
      jdbcUrl <- configuration.getString(s"db.${dbName}.url").toRight(s"db.$dbName.url is not set").right
      driver <- configuration.getString(s"db.${dbName}.driver").toRight(s"db.$dbName.driver is not set").right
    } yield {
      val (parsedUrl, parsedUser, parsedPass) = urlParser.parseUrl(jdbcUrl)
      val username = parsedUser
        .orElse(configuration.getString(s"db.${dbName}.username"))
        .orElse(configuration.getString(s"db.${dbName}.user"))
        .orNull
      val password = parsedPass
        .orElse(configuration.getString(s"db.${dbName}.password"))
        .orElse(configuration.getString(s"db.${dbName}.pass"))
        .orNull
      JdbcConfig(driver, parsedUrl, username, password)
    }

    jdbcConfigOrError match {
      case Left(message) =>
        logger.warn(message)
        None
      case Right(jdbcConfig) =>
        Some(jdbcConfig)
    }
  }

}
