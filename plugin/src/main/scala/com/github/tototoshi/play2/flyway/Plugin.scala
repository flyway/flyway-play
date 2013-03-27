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
package com.github.tototoshi.play2.flyway

import java.io.File
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import com.googlecode.flyway.core.Flyway
import com.googlecode.flyway.core.api.MigrationInfo
import play.core._
import org.apache.commons.io.FileUtils._

class Plugin(app: Application) extends play.api.Plugin
    with HandleWebCommandSupport
    with PluginConfiguration {

  val url = app.configuration.getString("db.default.url").getOrElse(throw new MigrationConfigurationException("db.default.url is not set."))
  val user = app.configuration.getString("db.default.user").orNull
  val password = app.configuration.getString("db.default.password").orNull

  private val flyway = new Flyway

  flyway.setDataSource(url, user, password)

  private val flywayPrefixToMigrationScript = "db/migration"

  private val playConfigDir = "conf"

  override lazy val enabled: Boolean = true

  private def migrationDescriptionToShow(migration: MigrationInfo): String = {
    val scriptPath = getFile(app.getFile("."), playConfigDir, flywayPrefixToMigrationScript, migration.getScript)
    s"""|--- ${migration.getScript} ---
    |${readFileToString(scriptPath)}""".stripMargin
  }

  private def checkState(): Unit = {
    val pendingMigrations = flyway.info().pending
    if (!pendingMigrations.isEmpty) {
      throw InvalidDatabaseRevision(
        "default",
        pendingMigrations.map(migrationDescriptionToShow).mkString("\n"))
    }
  }

  override def onStart(): Unit = {
    checkState()
  }

  override def onStop(): Unit = {
  }

  private def getRedirectUrlFromRequest(request: RequestHeader): String = {
    (for {
      urls <- request.queryString.get("redirect")
      url <- urls.headOption
    } yield url).getOrElse("/")
  }

  override def handleWebCommand(request: RequestHeader, sbtLink: SBTLink, path: java.io.File): Option[Result] = {
    if (request.path != applyPath) {
      checkState()
      None
    } else {
      flyway.migrate()
      sbtLink.forceReload()
      Some(Redirect(getRedirectUrlFromRequest(request)))
    }
  }

}
