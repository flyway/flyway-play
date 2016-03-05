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
