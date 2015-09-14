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

import play.core._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import org.flywaydb.core.Flyway

class FlywayWebCommand(
  configuration: Configuration,
  environment: Environment,
  flywayPrefixToMigrationScript: String,
  flyways: Map[DatabaseInfo, Flyway]
) extends HandleWebCommandSupport
    with WebCommandPath {

  override val baseURI = configuration.getString("flyway.webCommand.baseURI").getOrElse("")
  override val flywayPathName = configuration.getString("flyway.webCommand.baseName").getOrElse("@flyway")

  def handleWebCommand(request: RequestHeader, sbtLink: BuildLink, path: java.io.File): Option[Result] = {
    val css = {
      <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" type="text/css"/>
      <style>
        body {{
        font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;
      }}
      </style>
    }

    val js = {
      <script type="text/javascript" src="//cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
      <script type="text/javascript" src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>
    }

    val header = {
      <div class="navbar" role="navigation">
        <div class="container">
          <div class="navbar-header">
            <a class="navbar-brand" href={ webCommandBasePath }>play-flyway</a>
          </div>
        </div>
      </div>
    }

    request.path match {
      case migratePath(dbName, dbType) => {
        for {
          flyway <- getFlyway(dbName, dbType)
        } yield {
          flyway.migrate()
          sbtLink.forceReload()
          Redirect(getRedirectUrlFromRequest(request))
        }
      }
      case cleanPath(dbName, dbType) => {
        getFlyway(dbName, dbType).foreach(_.clean())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      }
      case repairPath(dbName, dbType) => {
        getFlyway(dbName, dbType).foreach(_.repair())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      }
      case versionedInitPath(dbName, version, dbType) => {
        getFlyway(dbName, dbType).foreach(_.setBaselineVersion(version))
        getFlyway(dbName, dbType).foreach(_.baseline())
        Some(Redirect(getRedirectUrlFromRequest(request)))
      }
      case showInfoPath(dbName, dbType) => {
        val description = for {
          flyway <- getFlyway(dbName, dbType).toList
          info <- flyway.info().all()
        } yield {
          val sql = environment.resourceAsStream(s"${flywayPrefixToMigrationScript}/${dbType}/${dbName}/${info.getScript}").map { in =>
            FileUtils.readInputStreamToString(in)
          }.orElse {
            for {
              script <- FileUtils.findJdbcMigrationFile(environment.rootPath, info.getScript)
            } yield FileUtils.readFileToString(script)
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

        val initLinks = for {
          flyway <- getFlyway(dbName, dbType).toList
          info <- flyway.info().all()
        } yield {
          val version = info.getVersion().getVersion()
          <li><a href={ withRedirectParam(versionedInitPath(dbName, version)) }>version: { version }</a></li>
        }

        val migratePathWithRedirectParam = withRedirectParam(migratePath(dbName))
        val cleanPathWithRedirectParam = withRedirectParam(cleanPath(dbName))
        val repairPathWithRedirectParam = withRedirectParam(repairPath(dbName))

        val html =
          <html>
            <head>
              <title>play-flyway</title>
              { css }
            </head>
            <body>
              { header }
              <div class="container">
                <a href={ baseURI + "/" }>&lt;&lt; Back to app</a>
                <h2>Database: { dbName }</h2>
                <a class="btn btn-primary" href={ migratePathWithRedirectParam }>migrate</a>
                <a class="btn btn-primary" href={ repairPathWithRedirectParam }>repair</a>
                <a class="btn btn-danger" href={ cleanPathWithRedirectParam }>clean</a>
                <!-- Split button -->
                <div class="btn-group">
                  <button type="button" class="btn btn-danger dropdown-toggle" data-toggle="dropdown">
                    init&nbsp;<span class="caret"></span>
                  </button>
                  <ul class="dropdown-menu" role="menu">
                    { initLinks }
                  </ul>
                </div>
                <!--<a style="color: red;" href={ initPathWithRedirectParam }>init</a>-->
                { description }
              </div>
              { js }
            </body>
          </html>

        Some(Ok(html).as("text/html"))

      }
      case `webCommandBasePath` => {
        val links = for {
          (dbName, flyway) <- flyways
          path = s"$webCommandBasePath/${dbName}"
        } yield {
          <ul>
            <li><a href={ path }>{ dbName }</a></li>
          </ul>
        }

        val html =
          <html>
            <head>
              <title>play-flyway</title>
              { css }
            </head>
            <body>
              { header }
              <div class="container">
                <a href={ baseURI + "/" }>&lt;&lt; Back to app</a>
                <div class="well">
                  { links }
                </div>
              </div>
              { js }
            </body>
          </html>

        Some(Ok(html).as("text/html"))
      }
      case _ => {
        None
      }
    }
  }

  private def getRedirectUrlFromRequest(request: RequestHeader): String = {
    (for {
      urls <- request.queryString.get("redirect")
      url <- urls.headOption
    } yield url).getOrElse("/")
  }

  private def getFlyway(dbName: String, dbType: String): Option[Flyway] = {
    flyways.find {
      case (DefaultDatabaseInfo(name, fw), _) if dbType == "db" && dbName == name => true
      case (SlickDatabaseInfo(slickName, slickFw), _) if dbType == "slick" && dbName == slickName => true
      case _ => false
    }.map(_._2)
  }
}
