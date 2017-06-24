package controllers

import javax.inject.{ Inject, Singleton }

import play.api.mvc._

@Singleton
class Application @Inject() () extends InjectedController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def hello = Action {
    Ok("Hello")
  }

}
