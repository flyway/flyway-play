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

import play.api._

case class InvalidDatabaseRevision(db: String, script: String) extends PlayException.RichDescription(
  "Database '" + db + "' needs migration!",
  "An SQL script need to be run on your database.")
    with WebCommandPath {

  def subTitle = "This SQL script must be run:"
  def content = script

  private val redirectToApply = s"""
    document.location = '${migratePath(db)}/?redirect=' + encodeURIComponent(location);
  """

  private val redirectToAdmin = s"""
    document.location = '/@flyway/' + encodeURIComponent('${db}')
  """

  def htmlDescription = {
    <span>An SQL script will be run on your database -</span>
    <input name="evolution-button" type="button" value="Apply this script now!" onclick={ redirectToApply }/>
    <input name="evolution-button" type="button" value="Other operations" onclick={ redirectToAdmin }/>
  }.mkString
}
