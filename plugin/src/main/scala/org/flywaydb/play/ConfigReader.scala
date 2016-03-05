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
import scala.util.control.Exception.catching

class ConfigReader(configuration: Configuration, environment: Environment) {

  val urlParser = new UrlParser(environment: Environment)

  lazy val logger = Logger(classOf[ConfigReader])

  private def getAllDatabaseNames: Seq[String] = (for {
    config <- configuration.getConfig("db").toList
    dbName <- config.subKeys
  } yield {
    dbName
  }).distinct

  def getFlywayConfigurations: Map[String, FlywayConfiguration] = {
    (for {
      dbName <- getAllDatabaseNames
      // Will only iterate if this is a configuration group, instead of a single value, allowing easy skipping of irrelevant entries
      subConfig <- catching(classOf[PlayException]).opt(configuration.getConfig("db." + dbName)).flatten

      // Skip silently if we've been told to
      if !subConfig.getBoolean("flywayIgnore").getOrElse(false)

      urlOption = subConfig.getString("url")
      driverOption = subConfig.getString("driver")

      // Issue appropriate warnings if we're provided only half the required information
      _ = if (urlOption.isDefined != driverOption.isDefined) {
        if (urlOption.isEmpty)
          logger.warn(s"Skipping db.$dbName, Found Driver, but No URL Specified; to inhibit this message, set db.$dbName.migration.flywayIgnore to true in your configuration.")
        else
          logger.warn(s"Skipping db.$dbName, Found URL, but No Driver Specified; to inhibit this message, set db.$dbName.migration.flywayIgnore to true in your configuration.")
      }

      // Skip if there's no URL or driver...
      fullUrl <- urlOption
      driver <- driverOption
    } yield {
      val (url, parsedUser, parsedPass) = urlParser.parseUrl(fullUrl)

      val user = parsedUser
        .orElse(subConfig.getString("username"))
        .orElse(subConfig.getString("user"))
        .orNull
      val password = parsedPass
        .orElse(subConfig.getString("password"))
        .orElse(subConfig.getString("pass"))
        .orNull
      val initOnMigrate =
        subConfig.getBoolean("migration.initOnMigrate").getOrElse(false)
      val validateOnMigrate =
        subConfig.getBoolean("migration.validateOnMigrate").getOrElse(true)
      val encoding =
        subConfig.getString("migration.encoding").getOrElse("UTF-8")
      val placeholderPrefix =
        subConfig.getString("migration.placeholderPrefix")
      val placeholderSuffix =
        subConfig.getString("migration.placeholderSuffix")

      val placeholders = {
        subConfig.getConfig("migration.placeholders").map { config =>
          config.subKeys.map { key => (key -> config.getString(key).getOrElse("")) }.toMap
        }.getOrElse(Map.empty)
      }

      val outOfOrder =
        subConfig.getBoolean("migration.outOfOrder").getOrElse(false)
      val auto =
        subConfig.getBoolean("migration.auto").getOrElse(false)
      val schemas =
        subConfig.getStringList("migration.schemas").getOrElse(java.util.Collections.emptyList[String]).asScala.toList
      val locations =
        subConfig.getStringList("migration.locations").getOrElse(java.util.Collections.emptyList[String]).asScala.toList

      val sqlMigrationPrefix =
        subConfig.getString("migration.sqlMigrationPrefix")

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
