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

import javax.inject.{Inject, Singleton}
import org.flywaydb.core.api.MigrationInfo
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import play.core._

class FlywayWebCommand(configuration: Configuration, environment: Environment, flyways: Flyways)
    extends HandleWebCommandSupport {

  private var checkedAlready = false

  def handleWebCommand(request: RequestHeader, sbtLink: BuildLink, path: java.io.File): Option[Result] = {

    request.path match {
      case WebCommandPath.migratePath(dbName) =>
        flyways.migrate(dbName)
        sbtLink.forceReload()
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.cleanPath(dbName) =>
        flyways.clean(dbName)
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.repairPath(dbName) =>
        flyways.repair(dbName)
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.versionedInitPath(dbName, version) =>
        flyways.baseline(dbName, version)
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.showInfoPath(dbName) =>
        val allMigrationInfo: Seq[MigrationInfo] = flyways.allMigrationInfo(dbName)
        val scriptsDirectory =
          configuration.getOptional[String](s"db.$dbName.migration.scriptsDirectory").getOrElse(dbName)
        val scripts: Seq[String] = allMigrationInfo.map { info =>
          environment
            .resourceAsStream(s"${flyways.flywayPrefixToMigrationScript}/$scriptsDirectory/${info.getScript}")
            .map { in =>
              FileUtils.readInputStreamToString(in)
            }
            .orElse {
              for {
                script <- FileUtils.findJdbcMigrationFile(environment.rootPath, info.getScript)
              } yield FileUtils.readFileToString(script)
            }
            .getOrElse("")
        }
        val showManualInsertQuery =
          configuration.getOptional[Boolean](s"db.$dbName.migration.showInsertQuery").getOrElse(false)
        val schemaTable = flyways.schemaTable(dbName)
        Some(
          Ok(views.html.info(request, dbName, allMigrationInfo, scripts, showManualInsertQuery, schemaTable))
            .as("text/html")
        )
      case "/@flyway" =>
        Some(Ok(views.html.index(flyways.allDatabaseNames)).as("text/html"))
      case _ =>
        synchronized {
          if (!checkedAlready) {
            for (dbName <- flyways.allDatabaseNames) {
              if (environment.mode == Mode.Test || flyways.config(dbName).auto) {
                flyways.migrate(dbName)
              } else {
                flyways.checkState(dbName)
              }
            }
            checkedAlready = true
          }
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
