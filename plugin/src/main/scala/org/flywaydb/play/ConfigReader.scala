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
  val playDbConfigPath = "play.db.config"
  val playSlickDbConfigPath = "play.slick.db.config"

  private def getAllDatabasePathWithNames: Seq[DatabaseInfo] = {
    val defaultConfigPath = configuration.getString(playDbConfigPath).getOrElse("")
    val slickConfigPath = configuration.getString(playSlickDbConfigPath).getOrElse("")

    def getDbNames(path: String, dbConfType: String = "default") = (for {
      config <- configuration.getConfig(path).toList
      dbName <- config.subKeys
    } yield {
      if (dbConfType == "slick") SlickDatabaseInfo(dbName, s"$slickConfigPath.$dbName.db")
      else DefaultDatabaseInfo(dbName, s"$defaultConfigPath.$dbName")
    }).distinct

    getDbNames(defaultConfigPath) ++ getDbNames(slickConfigPath, "slick")
  }

  def getFlywayConfigurations: Map[DatabaseInfo, FlywayConfiguration] = {
    (for {
      dbInfo <- getAllDatabasePathWithNames
    } yield {
      val (url, parsedUser, parsedPass) = configuration.getString(s"${dbInfo.configPath}.url").map(urlParser.parseUrl(_)).getOrElse(
        throw new MigrationConfigurationException(s"${dbInfo.configPath}.url is not set.")
      )

      val driver = configuration.getString(s"${dbInfo.configPath}.driver").getOrElse(
        throw new MigrationConfigurationException(s"${dbInfo.configPath}.driver is not set.")
      )

      val user = parsedUser
        .orElse(configuration.getString(s"${dbInfo.configPath}.username"))
        .orElse(configuration.getString(s"${dbInfo.configPath}.user"))
        .orNull

      val password = parsedPass
        .orElse(configuration.getString(s"${dbInfo.configPath}.password"))
        .orElse(configuration.getString(s"${dbInfo.configPath}.pass"))
        .orNull

      val initOnMigrate = configuration.getBoolean(s"${dbInfo.configPath}.migration.initOnMigrate").getOrElse(false)
      val validateOnMigrate = configuration.getBoolean(s"${dbInfo.configPath}.migration.validateOnMigrate").getOrElse(true)
      val encoding = configuration.getString(s"${dbInfo.configPath}.migration.encoding").getOrElse("UTF-8")
      val placeholderPrefix = configuration.getString(s"${dbInfo.configPath}.migration.placeholderPrefix")
      val placeholderSuffix = configuration.getString(s"${dbInfo.configPath}.migration.placeholderSuffix")

      val placeholders = configuration.getConfig(s"${dbInfo.configPath}.migration.placeholders").map { config =>
        config.subKeys.map { key => (key -> config.getString(key).getOrElse("")) }.toMap
      }.getOrElse(Map.empty)

      val outOfOrder = configuration.getBoolean(s"${dbInfo.configPath}.migration.outOfOrder").getOrElse(false)
      val auto = configuration.getBoolean(s"${dbInfo.configPath}.migration.auto").getOrElse(false)
      val schemas = configuration.getStringList(s"${dbInfo.configPath}.migration.schemas")
        .getOrElse(java.util.Collections.emptyList[String]).asScala.toList

      val database = DatabaseConfiguration(
        driver,
        url,
        user,
        password
      )

      dbInfo -> FlywayConfiguration(
        database,
        auto,
        initOnMigrate,
        validateOnMigrate,
        encoding,
        placeholderPrefix,
        placeholderSuffix,
        placeholders,
        outOfOrder,
        schemas
      )
    }).toMap

  }
}