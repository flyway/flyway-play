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

    request.path match {
      case WebCommandPath.migratePath(dbName) =>
        for {
          flyway <- flyways.get(dbName)
        } yield {
          flyway.migrate()
          sbtLink.forceReload()
          Redirect(getRedirectUrlFromRequest(request))
        }
      case WebCommandPath.cleanPath(dbName) =>
        flyways.get(dbName).foreach(_.clean())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.repairPath(dbName) =>
        flyways.get(dbName).foreach(_.repair())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.versionedInitPath(dbName, version) =>
        flyways.get(dbName).foreach(_.setBaselineVersionAsString(version))
        flyways.get(dbName).foreach(_.baseline())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      case WebCommandPath.showInfoPath(dbName) =>
        val allMigrationInfo: Seq[MigrationInfo] = flyways.get(dbName).toSeq.flatMap(_.info().all())
        val scripts: Seq[String] = allMigrationInfo.map { info =>
          environment.resourceAsStream(s"${flywayPrefixToMigrationScript}/${dbName}/${info.getScript}").map { in =>
            FileUtils.readInputStreamToString(in)
          }.orElse {
            for {
              script <- FileUtils.findJdbcMigrationFile(environment.rootPath, info.getScript)
            } yield FileUtils.readFileToString(script)
          }.getOrElse("")

          // create a insert sql script for the flyway schema table in order to apply migrations manually
          val showInsertQuery = configuration.getBoolean(s"db.${dbName}.migration.showInsertQuery").getOrElse(false)
          val schemaTable = flyway.getTable
          val insertSql = s"""INSERT INTO $schemaTable(version_rank, installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
           |SELECT MAX(version_rank)+1, MAX(installed_rank)+1, '${info.getVersion}', '${info.getDescription}', 'SQL', '${info.getScript}', ${info.getChecksum}, 'Manually', NOW(), 0, 1 from $schemaTable;
           |""".stripMargin

          val status = {
            if (info.getState.isApplied) {
              <span style="color: blue;">applied</span>
            } else if (info.getState.isFailed) {
              <span style="color: red;">failed</span>
            } else if (info.getState.isResolved) {
              <span style="color: green">resolved</span>
            }
          }

          <p>
            <h3>
              { info.getScript }
              ({ status }
              )
            </h3>
            <pre>{ sql }</pre>
            {
              if (showInsertQuery) {
                <h4>--- Manual insert ---</h4>
                <p class="text-muted">
                  If you need to apply your migrations manually use this SQL to update your flyway schema table.
                </p>
                <pre>{ insertSql }</pre>
              }
            }
          </p>

        }
        Some(Ok(views.html.info(request, dbName, allMigrationInfo, scripts)).as("text/html"))
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
