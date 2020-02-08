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

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class WebCommandPathSpec extends AnyFunSpec with Matchers {

  describe("PluginConfiguration") {

    describe("migratePath") {
      it("construct path to apply migration") {
        WebCommandPath.migratePath("foo") should be("/@flyway/foo/migrate")
      }
      it("extract db to migrate migration") {
        val dbName = "/@flyway/foo/migrate" match {
          case WebCommandPath.migratePath(db) => Some(db)
          case _ => None
        }
        dbName should be(Some("foo"))
      }
    }
  }
}
