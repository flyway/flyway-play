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

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import com.googlecode.flyway.core.Flyway
import com.googlecode.flyway.core.api.MigrationInfo
import com.googlecode.flyway.core.util.jdbc.DriverDataSource
import play.core._
import java.io.FileNotFoundException

class Plugin(implicit app: Application) extends play.api.Plugin
    with HandleWebCommandSupport
    with PluginConfiguration
    with FileUtils {

  private val configReader = new ConfigReader(app)

  private val allDatabaseNames = configReader.getDatabaseConfigurations.keys

  private val flywayPrefixToMigrationScript = "db/migration"

  private def initOnMigrate(dbName: String): Boolean =
    app.configuration.getBoolean(s"db.${dbName}.migration.initOnMigrate").getOrElse(false)

  private def migrationFileDirectoryExists(path: String): Boolean = {
    app.resource(path) match {
      case Some(r) => {
        Logger.debug(s"Directory for migration files found. ${path}")
        true
      }
      case None => {
        Logger.warn(s"Directory for migration files not found. ${path}")
        false
      }
    }
  }

  private val flyways: Map[String, Flyway] = {
    for {
      (dbName, configuration) <- configReader.getDatabaseConfigurations
      migrationFilesLocation = s"db/migration/${dbName}"
      if migrationFileDirectoryExists(migrationFilesLocation)
    } yield {
      val flyway = new Flyway
      flyway.setDataSource(new DriverDataSource(configuration.driver, configuration.url, configuration.user, configuration.password))
      flyway.setLocations(migrationFilesLocation)
      if (initOnMigrate(dbName)) {
        flyway.setInitOnMigrate(true)
      }
      dbName -> flyway
    }
  }

  override lazy val enabled: Boolean =
    !app.configuration.getString("flywayplugin").exists(_ == "disabled")

  private def migrationDescriptionToShow(dbName: String, migration: MigrationInfo): String = {
    app.resourceAsStream(s"${flywayPrefixToMigrationScript}/${dbName}/${migration.getScript}").map { in =>
      s"""|--- ${migration.getScript} ---
          |${readInputStreamToString(in)}""".stripMargin
    }.getOrElse(throw new FileNotFoundException(s"Migration file not found. ${migration.getScript}"))
  }

  private def checkState(dbName: String): Unit = {
    flyways.get(dbName).foreach { flyway =>
      val pendingMigrations = flyway.info().pending
      if (!pendingMigrations.isEmpty) {
        throw InvalidDatabaseRevision(
          dbName,
          pendingMigrations.map(migration => migrationDescriptionToShow(dbName, migration)).mkString("\n"))
      }
    }
  }

  override def onStart(): Unit = {
    for (dbName <- allDatabaseNames) {
      if (Play.isTest || app.configuration.getBoolean(s"db.${dbName}.migration.auto").getOrElse(false)) {
        migrateAutomatically(dbName)
      } else {
        checkState(dbName)
      }
    }
  }

  override def onStop(): Unit = {
  }

  private def migrateAutomatically(dbName: String): Unit = {
    flyways.get(dbName).foreach { flyway =>
      flyway.migrate()
    }
  }

  private def getRedirectUrlFromRequest(request: RequestHeader): String = {
    (for {
      urls <- request.queryString.get("redirect")
      url <- urls.headOption
    } yield url).getOrElse("/")
  }

  override def handleWebCommand(request: RequestHeader, sbtLink: BuildLink, path: java.io.File): Option[SimpleResult] = {

    val css = {
      <style>
        body {{
        width: 760px;
        margin: 20px auto 50px auto;
        font-family: "Helvetica Neue",Helvetica,Arial,sans-serif
        }}
        h2 {{ color: #000000; }}
        h3 {{ color: #000080; margin-left: 10px; }}
        h4 {{ color: #808000; margin-left: 20px; }}
        .container {{ margin-top: 20px; }}
      </style>
    }

    request.path match {
      case migratePath(dbName) => {
        for {
          flyway <- flyways.get(dbName)
        } yield {
          flyway.migrate()
          sbtLink.forceReload()
          Redirect(getRedirectUrlFromRequest(request))
        }
      }
      case cleanPath(dbName) => {
        flyways.get(dbName).foreach(_.clean())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      }
      case initPath(dbName) => {
        flyways.get(dbName).foreach(_.init())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      }
      case showInfoPath(dbName) => {
        val description = for {
          flyway <- flyways.get(dbName).toList
          info <- flyway.info().all()
        } yield {
          val sql = app.resourceAsStream(s"${flywayPrefixToMigrationScript}/${dbName}/${info.getScript}").map { in =>
            readInputStreamToString(in)
          }.getOrElse("")

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
          </p>
        }

        def withRedirectParam(path: String) = path + "?redirect=" + java.net.URLEncoder.encode(request.path, "utf-8")

        val migratePathWithRedirectParam = withRedirectParam(migratePath(dbName))
        val cleanPathWithRedirectParam = withRedirectParam(cleanPath(dbName))
        val initPathWithRedirectParam = withRedirectParam(initPath(dbName))

        val html =
          <html>
            <head>
              <title>play-flyway</title>
              { css }
            </head>
            <body>
              <h1><a href="/@flyway">play-flyway</a></h1>
              <a href="/">&lt;&lt; Back to app</a>
              <div class="container">
                <h2>Database: { dbName }</h2>
                <a style="color: blue;" href={ migratePathWithRedirectParam }>migrate</a>
                <a style="color: red;" href={ cleanPathWithRedirectParam }>clean</a>
                <a style="color: red;" href={ initPathWithRedirectParam }>init</a>
                { description }
              </div>
            </body>
          </html>

        Some(Ok(html).as("text/html"))

      }
      case "/@flyway" => {
        val links = for {
          (dbName, flyway) <- flyways
          path = s"/@flyway/${dbName}"
        } yield {
          <div>
            <a href={ path }>{ dbName }</a>
          </div>
        }

        val html =
          <html>
            <head>
              <title>play-flyway</title>
              { css }
            </head>
            <body>
              <h1><a href="/@flyway">play-flway</a></h1>
              <a href="/">&lt;&lt; Back to app</a>
              <div class="container">
                { links }
              </div>
            </body>
          </html>

        Some(Ok(html).as("text/html"))
      }
      case _ => {
        None
      }
    }
  }

}
