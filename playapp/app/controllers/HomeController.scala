package controllers

import javax.inject.{ Inject, Singleton }

import play.api.mvc._

@Singleton
class HomeController @Inject() (controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def hello = Action {
    Ok("Hello")
  }

}
