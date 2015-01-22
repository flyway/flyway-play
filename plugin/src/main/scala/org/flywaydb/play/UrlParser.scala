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

import play.api.{ Mode, Play }

/**
 * Most of the code is taken from package play.api.db.DB.
 */
trait UrlParser {
  val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  val MysqlCustomProperties = ".*\\?(.*)".r
  val H2DefaultUrl = "^jdbc:h2:mem:.+".r

  def parseUrl(url: String): Tuple3[String, Option[String], Option[String]] = {

    url match {
      case PostgresFullUrl(username, password, host, dbname) =>
        ("jdbc:postgresql://%s/%s".format(host, dbname), Some(username), Some(password))
      case url @ MysqlFullUrl(username, password, host, dbname) =>
        val defaultProperties = """?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"""
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        ("jdbc:mysql://%s/%s".format(host, dbname + addDefaultPropertiesIfNeeded), Some(username), Some(password))
      case url @ H2DefaultUrl() if !url.contains("DB_CLOSE_DELAY") =>
        val jdbcUrl = if (Play.maybeApplication.exists(_.mode == Mode.Dev)) {
          url + ";DB_CLOSE_DELAY=-1"
        } else {
          url
        }
        (jdbcUrl, None, None)
      case s: String =>
        (s, None, None)
    }

  }
}
