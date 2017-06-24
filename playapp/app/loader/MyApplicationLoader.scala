package loader

import org.flywaydb.play.FlywayPlayComponents
import play.api._
import play.api.ApplicationLoader.Context

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context)
    with FlywayPlayComponents
    with NoHttpFiltersComponents
    with _root_.controllers.AssetsComponents {
  flywayPlayInitializer
  lazy val applicationController = new _root_.controllers.Application()
  lazy val router = new _root_.router.Routes(httpErrorHandler, applicationController, assets)
}