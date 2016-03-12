package loader

import org.flywaydb.play.FlywayPlayComponents
import play.api._
import play.api.ApplicationLoader.Context

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) with FlywayPlayComponents {
  flywayPlayInitializer
  lazy val applicationController = new controllers.Application()
  lazy val assets = new controllers.Assets(httpErrorHandler)
  lazy val router = new _root_.router.Routes(httpErrorHandler, applicationController, assets)
}