/*
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

import org.scalatest.{ ShouldMatchers, FunSpec }

class UrlParserSpec extends FunSpec with ShouldMatchers {

  val urlParser = new UrlParser {}

  describe("UrlParser") {

    it("should parse URI that starts with 'postgres:'") {
      urlParser.parseUrl("postgres://john:secret@host.example.com/dbname") should be(
        ("jdbc:postgresql://host.example.com/dbname", Some("john"), Some("secret")))
    }

    it("should parse URI that starts with 'mysql:' and has no extra parameters") {
      urlParser.parseUrl("mysql://john:secret@host.example.com/dbname") should be(
        ("jdbc:mysql://host.example.com/dbname?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci", Some("john"), Some("secret")))
    }

    it("should parse URI that starts with 'mysql:' and has parameter(s)") {
      urlParser.parseUrl("mysql://john:secret@host.example.com/dbname?foo=bar") should be(
        ("jdbc:mysql://host.example.com/dbname?foo=bar", Some("john"), Some("secret")))
    }

    it("should return as is for URIs other than 'postgres' or 'mysql' ones") {
      urlParser.parseUrl("jdbc:yoursql://host.example.com/dbname") should be(
        ("jdbc:yoursql://host.example.com/dbname", None, None))
    }

  }

}
