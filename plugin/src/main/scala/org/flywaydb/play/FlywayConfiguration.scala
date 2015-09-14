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

case class FlywayConfiguration(
  database: DatabaseConfiguration,
  auto: Boolean,
  initOnMigrate: Boolean,
  validateOnMigrate: Boolean,
  encoding: String,
  placeholderPrefix: Option[String],
  placeholderSuffix: Option[String],
  placeholders: Map[String, String],
  outOfOrder: Boolean,
  schemas: List[String]
)

case class DatabaseConfiguration(
  driver: String,
  url: String,
  user: String,
  password: String
)

sealed trait DatabaseInfo {
  val name: String
  val configPath: String
}
case class SlickDatabaseInfo(name: String, configPath: String) extends DatabaseInfo
case class DefaultDatabaseInfo(name: String, configPath: String) extends DatabaseInfo