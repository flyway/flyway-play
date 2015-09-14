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
import org.scalatest.{ ShouldMatchers, FunSpec }
import org.scalatest.matchers._

class ConfigReaderSpec extends FunSpec with ShouldMatchers {
  val playDefaultConfig = Map(
    "play.db.config" -> "db",
    "play.slick.db.config" -> "slick.dbs"
  )

  val playMyPathConfig = playDefaultConfig ++ Map("play.db.config" -> "mydb.path")
  val playMySlickPathConfig = playDefaultConfig ++ Map("play.slick.db.config" -> "mySlick.path")

  val defaultDB = Map(
    "db.default.driver" -> "org.h2.Driver",
    "db.default.url" -> "jdbc:h2:mem:defaultDB;DB_CLOSE_DELAY=-1",
    "db.default.username" -> "sa"
  )

  val secondaryDB = Map(
    "db.secondary.driver" -> "org.h2.Driver",
    "db.secondary.url" -> "jdbc:h2:mem:secondaryDB;DB_CLOSE_DELAY=-1",
    "db.secondary.username" -> "sa",
    "db.secondary.password" -> "secret2"
  )

  val thirdDB = Map(
    "db.third.driver" -> "org.h2.Driver",
    "db.third.url" -> "jdbc:h2:mem:thirdDB;DB_CLOSE_DELAY=-1",
    "db.third.user" -> "sa",
    "db.third.pass" -> "secret3"
  )

  val differentPathDB = Map(
    "mydb.path.fourth.driver" -> "org.h2.Driver",
    "mydb.path.fourth.url" -> "jdbc:h2:mem:differentPathDB;DB_CLOSE_DELAY=-1",
    "mydb.path.fourth.user" -> "sa",
    "mydb.path.fourth.pass" -> "secret3"
  )

  val slickDefaultDB = Map(
    "slick.dbs.default.db.driver" -> "org.h2.Driver",
    "slick.dbs.default.db.url" -> "jdbc:h2:mem:slickDefaultDB;DB_CLOSE_DELAY=-1",
    "slick.dbs.default.db.user" -> "sa",
    "slick.dbs.default.db.pass" -> "secret3"
  )

  val slickDB = Map(
    "slick.dbs.fifth.db.driver" -> "org.h2.Driver",
    "slick.dbs.fifth.db.url" -> "jdbc:h2:mem:slickDB;DB_CLOSE_DELAY=-1",
    "slick.dbs.fifth.db.user" -> "sa",
    "slick.dbs.fifth.db.pass" -> "secret3"
  )

  val slickDifferentPathDB = Map(
    "mySlick.path.fifth.db.driver" -> "org.h2.Driver",
    "mySlick.path.fifth.db.url" -> "jdbc:h2:mem:slickDifferentPathDB;DB_CLOSE_DELAY=-1",
    "mySlick.path.fifth.db.user" -> "sa",
    "mySlick.path.fifth.db.pass" -> "secret3"
  )

  def genDbInfo(key: String, path: String = "db", isSlick: Boolean = false): DatabaseInfo =
    if (!isSlick) {
      DefaultDatabaseInfo(key, path + "." + key)
    } else {
      SlickDatabaseInfo(key, path + "." + key + ".db")
    }

  def withDefaultDB[A](additionalConfiguration: Map[String, Object], key: String = "default", path: String = "db", isSlick: Boolean = false)(assertion: FlywayConfiguration => A): A = {
    val configuration = Configuration((playDefaultConfig ++ defaultDB ++ slickDB ++ differentPathDB ++ additionalConfiguration).toSeq: _*)
    val environment = Environment.simple()
    val reader = new ConfigReader(configuration, environment)
    val configMap = reader.getFlywayConfigurations
    assertion(configMap.get(genDbInfo(key, path, isSlick)).get)
  }

  describe("ConfigReader") {

    it("should get database configurations for default driver") {
      val configuration = Configuration((playDefaultConfig ++ defaultDB ++ secondaryDB ++ thirdDB).toSeq: _*)
      val environment = Environment.simple()
      val reader = new ConfigReader(configuration, environment)
      val configMap = reader.getFlywayConfigurations

      configMap.get(genDbInfo("default")).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:defaultDB;DB_CLOSE_DELAY=-1", "sa", null))
      configMap.get(genDbInfo("secondary")).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:secondaryDB;DB_CLOSE_DELAY=-1", "sa", "secret2"))
      configMap.get(genDbInfo("third")).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:thirdDB;DB_CLOSE_DELAY=-1", "sa", "secret3"))
    }

    it("should get database configurations for different path driver") {
      val configuration = Configuration((playMyPathConfig ++ differentPathDB).toSeq: _*)
      val environment = Environment.simple()
      val reader = new ConfigReader(configuration, environment)
      val configMap = reader.getFlywayConfigurations
      configMap.get(genDbInfo("fourth", "mydb.path")).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:differentPathDB;DB_CLOSE_DELAY=-1", "sa", "secret3"))
    }

    it("should get database configurations for slick") {
      val configuration = Configuration((playDefaultConfig ++ slickDB ++ slickDefaultDB).toSeq: _*)
      val environment = Environment.simple()
      val reader = new ConfigReader(configuration, environment)
      val configMap = reader.getFlywayConfigurations
      configMap.get(genDbInfo("fifth", "slick.dbs", true)).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:slickDB;DB_CLOSE_DELAY=-1", "sa", "secret3"))
      configMap.get(genDbInfo("default", "slick.dbs", true)).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:slickDefaultDB;DB_CLOSE_DELAY=-1", "sa", "secret3"))
    }

    it("should get database configurations for slick with different path") {
      val configuration = Configuration((playMySlickPathConfig ++ slickDifferentPathDB).toSeq: _*)
      val environment = Environment.simple()
      val reader = new ConfigReader(configuration, environment)
      val configMap = reader.getFlywayConfigurations
      configMap.get(genDbInfo("fifth", "mySlick.path", true)).get.database should be(DatabaseConfiguration("org.h2.Driver", "jdbc:h2:mem:slickDifferentPathDB;DB_CLOSE_DELAY=-1", "sa", "secret3"))
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.auto" -> "true"), "fourth", "mydb.path") { config =>
          config.auto should be(true)
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.auto" -> "true"), "fifth", "slick.dbs", true) { config =>
          config.auto should be(true)
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.initOnMigrate" -> "true"), "fourth", "mydb.path") { config =>
          config.initOnMigrate should be(true)
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.initOnMigrate" -> "true"), "fifth", "slick.dbs", true) { config =>
          config.initOnMigrate should be(true)
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.validateOnMigrate" -> "false"), "fourth", "mydb.path") { config =>
          config.validateOnMigrate should be(false)
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.validateOnMigrate" -> "false"), "fifth", "slick.dbs", true) { config =>
          config.validateOnMigrate should be(false)
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.encoding" -> "EUC-JP"), "fourth", "mydb.path") { config =>
          config.encoding should be("EUC-JP")
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.encoding" -> "EUC-JP"), "fifth", "slick.dbs", true) { config =>
          config.encoding should be("EUC-JP")
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.placeholderPrefix" -> "PREFIX_"), "fourth", "mydb.path") { config =>
          config.placeholderPrefix should be(Some("PREFIX_"))
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.placeholderPrefix" -> "PREFIX_"), "fifth", "slick.dbs", true) { config =>
          config.placeholderPrefix should be(Some("PREFIX_"))
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.placeholderSuffix" -> "SUFFIX_"), "fourth", "mydb.path") { config =>
          config.placeholderSuffix should be(Some("SUFFIX_"))
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.placeholderSuffix" -> "SUFFIX_"), "fifth", "slick.dbs", true) { config =>
          config.placeholderSuffix should be(Some("SUFFIX_"))
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
            )
          )
        }
      }
      it("should be empty by default") {
        withDefaultDB(Map.empty) { config =>
          config.placeholders should be('empty)
        }
      }
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map(
          "mydb.path.fourth.migration.placeholders.fleetwood" -> "mac",
          "mydb.path.fourth.migration.placeholders.buckingham" -> "nicks"
        ), "fourth", "mydb.path") { config =>
          config.placeholders should be(
            Map(
              "fleetwood" -> "mac",
              "buckingham" -> "nicks"
            )
          )
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map(
          "slick.dbs.fifth.db.migration.placeholders.fleetwood" -> "mac",
          "slick.dbs.fifth.db.migration.placeholders.buckingham" -> "nicks"
        ), "fifth", "slick.dbs", true) { config =>
          config.placeholders should be(
            Map(
              "fleetwood" -> "mac",
              "buckingham" -> "nicks"
            )
          )
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.outOfOrder" -> "true"), "fourth", "mydb.path") { config =>
          config.outOfOrder should be(true)
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.outOfOrder" -> "true"), "fifth", "slick.dbs", true) { config =>
          config.outOfOrder should be(true)
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
      it("should be parsed using a different db path") {
        withDefaultDB(playMyPathConfig ++ Map("mydb.path.fourth.migration.schemas" -> List("public", "other")), "fourth", "mydb.path") { config =>
          config.schemas should be(List("public", "other"))
        }
      }
      it("should be parsed using slick") {
        withDefaultDB(Map("slick.dbs.fifth.db.migration.schemas" -> List("public", "other")), "fifth", "slick.dbs", true) { config =>
          config.schemas should be(List("public", "other"))
        }
      }
    }

  }
}
