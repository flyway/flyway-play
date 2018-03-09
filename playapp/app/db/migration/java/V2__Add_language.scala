package db.migration.java

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration

class V2__Add_language extends JdbcMigration {
  override def migrate(conn: Connection): Unit = {

    conn.createStatement().executeUpdate(
      """insert into language(id, name) values(1, 'SQL');
        |insert into language(id, name) values(2, 'Java');
       """.stripMargin)
  }
}
