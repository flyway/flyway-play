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

trait WebCommandPath {
  val baseURI: String = ""
  val flywayPathName = "@flyway"
  val webCommandBasePath = s"$baseURI/$flywayPathName"

  private val applyPathRegex = s"""$webCommandBasePath/(db|slick)/([a-zA-Z0-9_]+)/migrate""".r
  private val showInfoPathRegex = s"""$webCommandBasePath/(db|slick)/([a-zA-Z0-9_]+)""".r
  private val cleanPathRegex = s"""$webCommandBasePath/(db|slick)/([a-zA-Z0-9_]+)/clean""".r
  private val repairPathRegex = s"""$webCommandBasePath/(db|slick)/([a-zA-Z0-9_]+)/repair""".r
  private val initPathRegex = s"""$webCommandBasePath/(db|slick)/([a-zA-Z0-9_]+)/init/""".r
  private val versionedInitPathRegex = s"""$webCommandBasePath/(db|slick)/([a-zA-Z0-9_]+)/init/([0-9.]+)""".r

  object migratePath {
    def apply(dbName: String, dbType: String = "db"): String = {
      s"$webCommandBasePath/${dbType}/${dbName}/migrate"
    }

    def unapply(path: String): Option[(String, String)] = {
      applyPathRegex.findFirstMatchIn(path).map(v => v.group(2) -> v.group(1))
    }
  }

  object showInfoPath {
    def unapply(path: String): Option[(String, String)] = {
      showInfoPathRegex.findFirstMatchIn(path).map(v => v.group(2) -> v.group(1))
    }
  }

  object cleanPath {
    def apply(dbName: String, dbType: String = "db"): String = {
      s"$webCommandBasePath/${dbName}/clean"
    }

    def unapply(path: String): Option[(String, String)] = {
      cleanPathRegex.findFirstMatchIn(path).map(v => v.group(2) -> v.group(1))
    }
  }

  object repairPath {
    def apply(dbName: String, dbType: String = "db"): String = {
      s"$webCommandBasePath/${dbName}/repair"
    }

    def unapply(path: String): Option[(String, String)] = {
      repairPathRegex.findFirstMatchIn(path).map(v => v.group(2) -> v.group(1))
    }
  }

  object versionedInitPath {
    def apply(dbName: String, version: String, dbType: String = "db"): String = {
      s"$webCommandBasePath/${dbName}/init/${version}"
    }

    def unapply(path: String): Option[(String, String, String)] = {
      versionedInitPathRegex.findFirstMatchIn(path) match {
        case None => None
        case Some(matched) => Some(matched.group(2), matched.group(3), matched.group(1))
      }
    }
  }

  object initPath {
    def apply(dbName: String, dbType: String = "db"): String = s"$webCommandBasePath/${dbName}/init"
    def unapply(path: String): Option[(String, String)] =
      initPathRegex.findFirstMatchIn(path).map(v => v.group(2) -> v.group(1))
  }
}