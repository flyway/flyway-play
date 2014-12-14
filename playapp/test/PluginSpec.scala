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

import org.scalatest.FunSpec
import org.scalatest.matchers._
import play.api.test._
import play.api.test.Helpers._
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

class PluginSpec extends FunSpec
    with ShouldMatchers {

  def fixture = new {

  }

  def test() = {
    DB autoCommit { implicit session =>

      val people =
        sql"SELECT * FROM person"
          .map(rs => rs.int("id") -> rs.string("name"))
          .list
          .apply()

      people.size should be(4)

      sql"DROP TABLE person".execute.apply()

      // Table created by flyway
      sql"""DROP TABLE "schema_version"""".execute.apply()
    }

    NamedDB('secondary) autoCommit { implicit session =>
      val person =
        sql"SELECT * FROM job"
          .map(rs => rs.int("id") -> rs.string("name"))
          .list
          .apply()

      person.size should be(3)

      sql"DROP TABLE job".execute.apply()

      // Table created by flyway
      sql"""DROP TABLE "schema_version"""".execute.apply()
    }

    NamedDB('placeholders) autoCommit { implicit session =>
      val wows =
        sql"SELECT * FROM wow" // This table name is substituted for a placeholder during migration
          .map(rs => rs.int("id") -> rs.string("name"))
          .list
          .apply()

      wows.size should be(1)
      wows.head should be((1, "Oh!"))

      sql"DROP TABLE wow".execute.apply()

      // Table created by flyway
      sql"""DROP TABLE "schema_version"""".execute.apply()
    }

    NamedDB('java) autoCommit { implicit session =>
      val languages =
        sql"SELECT * FROM language"
          .map(rs => rs.int("id") -> rs.string("name"))
          .list
          .apply()

      languages.size should be(2)

      sql"DROP TABLE language".execute.apply()

      // Table created by flyway
      sql"""DROP TABLE "schema_version"""".execute.apply()
    }

  }

  describe("Plugin") {

    it("should migrate automatically when testing") {
      running(FakeApplication(additionalPlugins = Seq("scalikejdbc.PlayPlugin"))) {
        test()
      }
    }

    it("should work fine with in-memory databases.") {

      running(FakeApplication(
        additionalConfiguration =
          inMemoryDatabase(name = "default", Map("DB_CLOSE_DELAY" -> "-1")) ++
            inMemoryDatabase(name = "secondary", Map("DB_CLOSE_DELAY" -> "-1")),
        additionalPlugins = Seq("scalikejdbc.PlayPlugin"))) {
        test()
      }
    }

  }
}

