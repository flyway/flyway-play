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

  val urlParser = new UrlParser(environment: Environment)

  private def getAllDatabaseNames: Seq[String] = (for {
    config <- configuration.getConfig("db").toList
    dbName <- config.subKeys
  } yield {
    dbName
  }).distinct

  def getFlywayConfigurations: Map[String, FlywayConfiguration] = {
    (for {
      dbName <- getAllDatabaseNames
    } yield {
      val (url, parsedUser, parsedPass) = configuration.getString(s"db.${dbName}.url").map(urlParser.parseUrl(_)).getOrElse(
        throw new MigrationConfigurationException(s"db.${dbName}.url is not set.")
      )
      val driver = configuration.getString(s"db.${dbName}.driver").getOrElse(
        throw new MigrationConfigurationException(s"db.${dbName}.driver is not set.")
      )
      val user = parsedUser
        .orElse(configuration.getString(s"db.${dbName}.username"))
        .orElse(configuration.getString(s"db.${dbName}.user"))
        .orNull
      val password = parsedPass
        .orElse(configuration.getString(s"db.${dbName}.password"))
        .orElse(configuration.getString(s"db.${dbName}.pass"))
        .orNull
      val initOnMigrate =
        configuration.getBoolean(s"db.${dbName}.migration.initOnMigrate").getOrElse(false)
      val validateOnMigrate =
        configuration.getBoolean(s"db.${dbName}.migration.validateOnMigrate").getOrElse(true)
      val encoding =
        configuration.getString(s"db.${dbName}.migration.encoding").getOrElse("UTF-8")
      val placeholderPrefix =
        configuration.getString(s"db.${dbName}.migration.placeholderPrefix")
      val placeholderSuffix =
        configuration.getString(s"db.${dbName}.migration.placeholderSuffix")

      val placeholders = {
        configuration.getConfig(s"db.${dbName}.migration.placeholders").map { config =>
          config.subKeys.map { key => (key -> config.getString(key).getOrElse("")) }.toMap
        }.getOrElse(Map.empty)
      }

      val outOfOrder =
        configuration.getBoolean(s"db.${dbName}.migration.outOfOrder").getOrElse(false)
      val auto =
        configuration.getBoolean(s"db.${dbName}.migration.auto").getOrElse(false)
      val schemas =
        configuration.getStringList(s"db.${dbName}.migration.schemas").getOrElse(java.util.Collections.emptyList[String]).asScala.toList
      val locations =
        configuration.getStringList(s"db.${dbName}.migration.locations").getOrElse(java.util.Collections.emptyList[String]).asScala.toList

      val sqlMigrationPrefix =
        configuration.getString(s"db.${dbName}.migration.sqlMigrationPrefix")

      val database = DatabaseConfiguration(
        driver,
        url,
        user,
        password)

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

}
