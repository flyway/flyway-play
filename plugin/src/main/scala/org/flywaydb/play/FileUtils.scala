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

import java.io.{ File, InputStream }

object FileUtils {

  def readFileToString(filename: File): String = {
    val src = scala.io.Source.fromFile(filename, "UTF-8")
    try {
      src.mkString
    } finally {
      src.close()
    }
  }

  def readInputStreamToString(in: InputStream): String = {
    val src = scala.io.Source.fromInputStream(in, "UTF-8")
    try {
      src.mkString
    } finally {
      src.close()
      in.close()
    }
  }

  def recursiveListFiles(root: File): Seq[File] = {
    if (!root.isDirectory) {
      throw new IllegalArgumentException(s"root is not a directory")
    }
    val these = root.listFiles.toSeq
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles).toSeq
  }

  def findFile(root: File, filename: String): Option[File] = {
    recursiveListFiles(root).dropWhile(f => f.getName != filename).headOption
  }

  private def findSourceFile(root: File, className: String, ext: String): Option[File] = {
    for {
      cls <- className.split("\\.").lastOption
      filename = cls + "." + ext
      f <- findFile(root, filename)
    } yield f
  }

  private def findJavaSourceFile(root: File, className: String): Option[File] = {
    findSourceFile(root, className, "java")
  }

  private def findScalaSourceFile(root: File, className: String): Option[File] = {
    findSourceFile(root, className, "scala")
  }

  def findJdbcMigrationFile(root: File, className: String): Option[File] = {
    findScalaSourceFile(root, className).orElse(findJavaSourceFile(root, className))
  }
}
