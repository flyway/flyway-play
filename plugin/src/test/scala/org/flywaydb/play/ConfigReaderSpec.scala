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

import play.api.{ Environment, Configuration }
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.FunSpec
import org.scalatest.matchers._

class ConfigReaderSpec extends FunSpec with ShouldMatchers {

  val defaultDB = Map(
    "db.default.driver" -> "org.h2.Driver",
    "db.default.url" -> "jdbc:h2:mem:example;DB_CLOSE_DELAY=-1",
    "db.default.username" -> "sa"
  )

  val secondaryDB = Map(
    "db.secondary.driver" -> "org.h2.Driver",
    "db.secondary.url" -> "jdbc:h2:mem:example2;DB_CLOSE_DELAY=-1",
    "db.secondary.username" -> "sa",
    "db.secondary.password" -> "secret2"
  )

  val thirdDB = Map(
    "db.third.driver" -> "org.h2.Driver",
    "db.third.url" -> "jdbc:h2:mem:example3;DB_CLOSE_DELAY=-1",
    "db.third.user" -> "sa",
    "db.third.pass" -> "secret3"
  )

  val not_a_DB = Map(
    // Not a group
    "db.fake" → "This isn't a database",
    // Not a database entry (no url, no driver)
    "db.boneCP.autoCommit" → "true"
  )

  val fourthDB = Map(
    "db.fourth.driver" -> "org.h2.Driver",
    "db.fourth.url" -> "jdbc:h2:mem:example4;DB_CLOSE_DELAY=-1",
    "db.fourth.user" -> "sa",
    "db.fourth.pass" -> "secret4",
    "db.fourth.flywayIgnore" -> "true"
  )

  def withDefaultDB[A](additionalConfiguration: Map[String, Object])(assertion: FlywayConfiguration => A): A = {
    val configuration = Configuration((defaultDB ++ additionalConfiguration).toSeq: _*)
    val environment = Environment.simple()
    val reader = new ConfigReader(configuration, environment)
    val configMap = reader.getFlywayConfigurations
    assertion(configMap.get("default").get)
  }

  describe("ConfigReader") {

    it("should get database configurations") {
      val configuration = Configuration((defaultDB ++ secondaryDB ++ thirdDB ++ not_a_DB).toSeq: _*)
      val environment = Environment.simple()
      val reader = new ConfigReader(configuration, environment)
      val configMap = reader.getFlywayConfigurations
      configMap.get("default").get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:example;DB_CLOSE_DELAY=-1", "sa", null))
      configMap.get("secondary").get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:example2;DB_CLOSE_DELAY=-1", "sa", "secret2"))
      configMap.get("third").get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:example3;DB_CLOSE_DELAY=-1", "sa", "secret3"))
    }

    describe("auto") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.auto" -> "true")) { config =>
          config.auto should be(true)
        }
      }
      it("should be false by default") {
        withDefaultDB(Map.empty) { config =>
          config.auto should be(false)
        }
      }
    }

    describe("initOnMigrate") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.initOnMigrate" -> "true")) { config =>
          config.initOnMigrate should be(true)
        }
      }
      it("should be false by default") {
        withDefaultDB(Map.empty) { config =>
          config.initOnMigrate should be(false)
        }
      }
    }

    describe("validateOnMigrate") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.validateOnMigrate" -> "false")) { config =>
          config.validateOnMigrate should be(false)
        }
      }
      it("should be true by default") {
        withDefaultDB(Map.empty) { config =>
          config.validateOnMigrate should be(true)
        }
      }
    }

    describe("encoding") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.encoding" -> "EUC-JP")) { config =>
          config.encoding should be("EUC-JP")
        }
      }
      it("should be UTF-8 by default") {
        withDefaultDB(Map.empty) { config =>
          config.encoding should be("UTF-8")
        }
      }
    }

    describe("placeholderPrefix") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.placeholderPrefix" -> "PREFIX_")) { config =>
          config.placeholderPrefix should be(Some("PREFIX_"))
        }
      }
      it("should be None by default") {
        withDefaultDB(Map.empty) { config =>
          config.placeholderPrefix should be(None)
        }
      }
    }

    describe("placeholderSuffix") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.placeholderSuffix" -> "SUFFIX_")) { config =>
          config.placeholderSuffix should be(Some("SUFFIX_"))
        }
      }
      it("should be None by default") {
        withDefaultDB(Map.empty) { config =>
          config.placeholderSuffix should be(None)
        }
      }
    }

    describe("placeholder") {
      it("should be parsed") {
        withDefaultDB(Map(
          "db.default.migration.placeholders.fleetwood" -> "mac",
          "db.default.migration.placeholders.buckingham" -> "nicks"
        )) { config =>
          config.placeholders should be(
            Map(
              "fleetwood" -> "mac",
              "buckingham" -> "nicks"
            ))
        }
      }
      it("should be empty by default") {
        withDefaultDB(Map.empty) { config =>
          config.placeholders should be('empty)
        }
      }
    }

    describe("outOfOrder") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.outOfOrder" -> "true")) { config =>
          config.outOfOrder should be(true)
        }
      }
      it("should be false by default") {
        withDefaultDB(Map.empty) { config =>
          config.outOfOrder should be(false)
        }
      }
    }

    describe("schemas") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.schemas" -> List("public", "other"))) { config =>
          config.schemas should be(List("public", "other"))
        }
      }
      it("should be None by default") {
        withDefaultDB(Map.empty) { config =>
          config.schemas should be(List.empty)
        }
      }
    }

    describe("locations") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.locations" -> List("h2", "common"))) { config =>
          config.locations should be(List("h2", "common"))
        }
      }
      it("should be None by default") {
        withDefaultDB(Map.empty) { config =>
          config.locations should be(List.empty)
        }
      }
    }

    describe("sqlMigrationPrefix") {
      it("should be parsed") {
        withDefaultDB(Map("db.default.migration.sqlMigrationPrefix" -> "migration_")) { config =>
          config.sqlMigrationPrefix should be(Some("migration_"))
        }
      }
      it("should be None by default") {
        withDefaultDB(Map.empty) { config =>
          config.sqlMigrationPrefix should be(None)
        }
      }
    }

    it("should skip sections marked ignore") {
      val configuration = Configuration((defaultDB ++ secondaryDB ++ thirdDB ++ fourthDB ++ not_a_DB).toSeq: _*)
      val environment = Environment.simple()
      val reader = new ConfigReader(configuration, environment)
      val configMap = reader.getFlywayConfigurations
      configMap.get("default").get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:example;DB_CLOSE_DELAY=-1", "sa", null))
      configMap.get("secondary").get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:example2;DB_CLOSE_DELAY=-1", "sa", "secret2"))
      configMap.get("third").get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:example3;DB_CLOSE_DELAY=-1", "sa", "secret3"))
      configMap.contains("fourth") should be(false)
    }

  }
}
