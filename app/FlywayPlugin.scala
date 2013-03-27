package flyway

import java.io.File
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import com.googlecode.flyway.core.Flyway
import com.googlecode.flyway.core.api.MigrationInfo
import play.core._
import org.apache.commons.io.FileUtils._

class MigrationConfigurationException(val message: String) extends Exception(message)

class FlywayPlugin(app: Application) extends Plugin with HandleWebCommandSupport {

  val url = app.configuration.getString("db.default.url").getOrElse(throw new MigrationConfigurationException("db.default.url is not set."))
  val user = app.configuration.getString("db.default.user").orNull
  val password = app.configuration.getString("db.default.password").orNull

  val flyway = new Flyway

  flyway.setDataSource(url, user, password)

  private val applyPath = "/@flyway/apply"

  override lazy val enabled: Boolean = true

  private def getScriptPathFromScriptName(scriptName: String): String =
    "conf/db/migration/" + scriptName

  private def migrationDescriptionToShow(migration: MigrationInfo): String = {
    val scriptPath = getScriptPathFromScriptName(migration.getScript)
    s"""|--- ${scriptPath} ---
    |${readFileToString(new File(scriptPath))}""".stripMargin
  }

  private def checkState(): Unit = {
    val pendingMigrations = flyway.info().pending
    if (! pendingMigrations.isEmpty) {
      throw InvalidDatabaseRevision(
        "default",
        pendingMigrations.map(migrationDescriptionToShow).mkString("\n"))
    }
  }

  override def onStart(): Unit = {
    checkState()
  }

  override def onStop(): Unit = {
  }

  private def getRedirectUrlFromRequest(request: RequestHeader): String = {
    (for {
      urls <- request.queryString.get("redirect")
      url <- urls.headOption
    } yield url).getOrElse("/")
  }

  override def handleWebCommand(request: RequestHeader, sbtLink: SBTLink, path: java.io.File): Option[Result] = {
    if (request.path != applyPath) {
      checkState()
      None
    } else {
      flyway.migrate()
      sbtLink.forceReload()
      Some(Redirect(getRedirectUrlFromRequest(request)))
    }
  }

  case class InvalidDatabaseRevision(db: String, script: String) extends PlayException.RichDescription(
    "Database '" + db + "' needs migration!",
    "An SQL script need to be run on your database.") {

      def subTitle = "This SQL script must be run:"
      def content = script

      private val javascript = s"""
      document.location = '${applyPath}?redirect=' + encodeURIComponent(location);
      """

      def htmlDescription = {
        <span>An SQL script will be run on your database -</span>
        <input name="evolution-button" type="button" value="Apply this script now!" onclick={ javascript }/>
      }.toString
    }


}
