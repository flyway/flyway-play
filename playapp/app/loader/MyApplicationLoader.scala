package loader

import org.flywaydb.play.FlywayPlayComponents
import play.api._
import _root_.controllers.HomeController
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context)
    with FlywayPlayComponents
    with HttpFiltersComponents
    with _root_.controllers.AssetsComponents {
  flywayPlayInitializer
  lazy val applicationController = new HomeController(controllerComponents)
  lazy val router = new _root_.router.Routes(httpErrorHandler, applicationController, assets)
}