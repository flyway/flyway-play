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

import org.scalatest.{ ShouldMatchers, FunSpec }
import java.io.{ File, FileInputStream }

class FileUtilsSpec extends FunSpec with ShouldMatchers {

  describe("FileUtils") {

    it("should read a File to String") {
      val f = new File("plugin/src/test/resources/sample.sql")
      val s = FileUtils.readFileToString(f)
      s should be("""|create table person (
                     |    id int not null,
                     |    name varchar(100) not null
                     |);
                     |""".stripMargin)
    }

    it("should read InputStream to String") {
      val f = new FileInputStream("plugin/src/test/resources/sample.sql")
      val s = FileUtils.readInputStreamToString(f)
      s should be("""|create table person (
                     |    id int not null,
                     |    name varchar(100) not null
                     |);
                     |""".stripMargin)
    }

    it("should find files recursively") {
      val temp = File.createTempFile("flyway-play-", "-test");
      temp.delete()
      temp.mkdir()
      val sub1 = new File(temp, "sub1")
      sub1.mkdir()
      val sub2 = new File(sub1, "sub2")
      sub2.mkdir()
      val testfile1 = new File(sub2, "AAA.java")
      testfile1.createNewFile()
      val testfile2 = new File(sub2, "BBB.scala")
      testfile2.createNewFile()

      FileUtils.recursiveListFiles(temp) should be(
        Seq(sub1, sub2, testfile1, testfile2)
      )

      testfile1.delete()
      testfile2.delete()
      sub2.delete()
      sub1.delete()
      temp.delete()
    }

    it("should find a file in file tree") {
      val temp = File.createTempFile("flyway-play-", "-test");
      temp.delete()
      temp.mkdir()
      val sub1 = new File(temp, "sub1")
      sub1.mkdir()
      val sub2 = new File(sub1, "sub2")
      sub2.mkdir()
      val testfile1 = new File(sub2, "AAA.java")
      testfile1.createNewFile()
      val testfile2 = new File(sub2, "BBB.scala")
      testfile2.createNewFile()

      FileUtils.findFile(temp, "AAA.java") should be(Some(testfile1))

      testfile1.delete()
      testfile2.delete()
      sub2.delete()
      sub1.delete()
      temp.delete()
    }

    it("should find a java/scala file in file tree") {
      val temp = File.createTempFile("flyway-play-", "-test");
      temp.delete()
      temp.mkdir()
      val sub1 = new File(temp, "sub1")
      sub1.mkdir()
      val sub2 = new File(sub1, "sub2")
      sub2.mkdir()
      val testfile1 = new File(sub2, "AAA.java")
      testfile1.createNewFile()
      val testfile2 = new File(sub2, "BBB.scala")
      testfile2.createNewFile()

      FileUtils.findJdbcMigrationFile(temp, "org.flywaydb.flyway.AAA") should be(Some(testfile1))
      FileUtils.findJdbcMigrationFile(temp, "org.flywaydb.flyway.BBB") should be(Some(testfile2))

      testfile1.delete()
      testfile2.delete()
      sub2.delete()
      sub1.delete()
      temp.delete()
    }

  }

}
