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

trait PluginConfiguration {
  private val applyPathRegex = s"""/@flyway/([a-zA-Z0-9_]+)/migrate""".r
  private val showInfoPathRegex = """/@flyway/([a-zA-Z0-9_]+)""".r
  private val cleanPathRegex = """/@flyway/([a-zA-Z0-9_]+)/clean""".r
  private val initPathRegex = """/@flyway/([a-zA-Z0-9_]+)/init/""".r
  private val versionedInitPathRegex = """/@flyway/([a-zA-Z0-9_]+)/init/([0-9.]+)""".r

  object migratePath {

    def apply(dbName: String): String = {
      s"/@flyway/${dbName}/migrate"
    }

    def unapply(path: String): Option[String] = {
      applyPathRegex.findFirstMatchIn(path).map(_.group(1))
    }

  }

  object showInfoPath {

    def unapply(path: String): Option[String] = {
      showInfoPathRegex.findFirstMatchIn(path).map(_.group(1))
    }

  }

  object cleanPath {

    def apply(dbName: String): String = {
      s"/@flyway/${dbName}/clean"
    }

    def unapply(path: String): Option[String] = {
      cleanPathRegex.findFirstMatchIn(path).map(_.group(1))
    }

  }

  object versionedInitPath {
    def apply(dbName: String, version: String): String = {
      s"/@flyway/${dbName}/init/${version}"
    }

    def unapply(path: String): Option[(String, String)] = {
      versionedInitPathRegex.findFirstMatchIn(path) match {
        case None => None
        case Some(matched) => Some(matched.group(1), matched.group(2))
      }
    }
  }

  object initPath {

    def apply(dbName: String): String = {
      s"/@flyway/${dbName}/init"
    }

    def unapply(path: String): Option[String] = {
      initPathRegex.findFirstMatchIn(path).map(_.group(1))
    }

  }

}
