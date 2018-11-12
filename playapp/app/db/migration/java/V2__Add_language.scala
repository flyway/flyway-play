package db.migration.java

import org.flywaydb.core.api.migration.{ BaseJavaMigration, Context }

class V2__Add_language extends BaseJavaMigration {

  override def migrate(context: Context): Unit = {
    val conn = context.getConnection
    conn.createStatement().executeUpdate(
      """insert into language(id, name) values(1, 'SQL');
        |insert into language(id, name) values(2, 'Java');
      """.stripMargin)
  }

}
