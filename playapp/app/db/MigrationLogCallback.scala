package db

import java.sql.Connection

import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.callback.FlywayCallback

case class MigrationLogCallback() extends FlywayCallback {

  override def beforeClean(connection: Connection) = ()

  override def afterInfo(connection: Connection) = ()

  override def beforeInit(connection: Connection) = ()

  override def beforeRepair(connection: Connection) = ()

  override def afterRepair(connection: Connection) = ()

  override def afterInit(connection: Connection) = ()

  override def afterValidate(connection: Connection) = ()

  override def beforeEachMigrate(connection: Connection, info: MigrationInfo) = println(s"Applying migration to version:${info.getVersion} description: ${info.getDescription}")

  override def afterEachMigrate(connection: Connection, info: MigrationInfo) = println(s"Applied  migration to version:${info.getVersion} description: ${info.getDescription}")

  override def afterMigrate(connection: Connection) = ()

  override def beforeValidate(connection: Connection) = ()

  override def beforeInfo(connection: Connection) = ()

  override def afterClean(connection: Connection) = ()

  override def beforeMigrate(connection: Connection) = ()

  override def afterBaseline(c: Connection) = ()

  override def beforeBaseline(c: Connection) = ()
}
