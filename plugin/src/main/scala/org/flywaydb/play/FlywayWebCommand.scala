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

import org.flywaydb.core.api.MigrationInfo
import play.core._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import org.flywaydb.core.Flyway

class FlywayWebCommand(
  configuration: Configuration,
  environment: Environment,
  flywayPrefixToMigrationScript: String,
  flyways: Map[String, Flyway])
  extends HandleWebCommandSupport {

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
          flyway.setBaselineVersionAsString(version)
          flyway.baseline()
          Redirect(getRedirectUrlFromRequest(request))
        }
        Some(result)
      case WebCommandPath.showInfoPath(dbName) =>
        val result = withDatabase(dbName) { flyway =>
          val allMigrationInfo: Seq[MigrationInfo] = flyways.get(dbName).toSeq.flatMap(_.info().all())
          val dbFolder = configuration.getOptional[String](s"db.$dbName.migration.dbFolder").getOrElse(dbName)
          val scripts: Seq[String] = allMigrationInfo.map { info =>
            environment.resourceAsStream(s"$flywayPrefixToMigrationScript/$dbFolder/${info.getScript}").map { in =>
              FileUtils.readInputStreamToString(in)
            }.orElse {
              for {
                script <- FileUtils.findJdbcMigrationFile(environment.rootPath, info.getScript)
              } yield FileUtils.readFileToString(script)
            }.getOrElse("")
          }
          val showManualInsertQuery = configuration.getOptional[Boolean](s"db.$dbName.migration.showInsertQuery").getOrElse(false)
          val schemaTable = flyway.getTable
          Ok(views.html.info(request, dbName, allMigrationInfo, scripts, showManualInsertQuery, schemaTable)).as("text/html")
        }
        Some(result)
      case "/@flyway" =>
        Some(Ok(views.html.index(flyways.keys.toSeq)).as("text/html"))
      case _ =>
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
