package db.migration.java

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration

class V1__Create_language_table extends JdbcMigration {
  override def migrate(conn: Connection): Unit = {

    conn.createStatement().executeUpdate(
      """create table language (
        |    id integer primary key,
        |    name varchar(100) not null
        |);""".stripMargin)
  }
}
