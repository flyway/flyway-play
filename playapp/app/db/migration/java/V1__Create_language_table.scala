package db.migration.java

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V1__Create_language_table extends BaseJavaMigration {

  override def migrate(context: Context): Unit = {
    val conn = context.getConnection
    conn
      .createStatement()
      .executeUpdate("""create table language (
        |    id integer primary key,
        |    name varchar(100) not null
        |);""".stripMargin)
  }

}
