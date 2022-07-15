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
    validateOnStart: Boolean,
    auto: Boolean,
    locations: Seq[String],
    encoding: Option[String],
    schemas: Seq[String],
    table: Option[String],
    placeholderReplacement: Option[Boolean],
    placeholders: Map[String, String],
    placeholderPrefix: Option[String],
    placeholderSuffix: Option[String],
    sqlMigrationPrefix: Option[String],
    repeatableSqlMigrationPrefix: Option[String],
    sqlMigrationSeparator: Option[String],
    sqlMigrationSuffix: Option[String],
    sqlMigrationSuffixes: Seq[String],
    ignoreFutureMigrations: Option[Boolean],
    ignoreMissingMigrations: Option[Boolean],
    ignoreMigrationPatterns: Seq[String],
    validateOnMigrate: Option[Boolean],
    cleanOnValidationError: Option[Boolean],
    cleanDisabled: Option[Boolean],
    initOnMigrate: Option[Boolean],
    outOfOrder: Option[Boolean],
    scriptsDirectory: Option[String],
    mixed: Option[Boolean],
    group: Option[Boolean]
)

case class DatabaseConfiguration(driver: String, url: String, user: String, password: String)
