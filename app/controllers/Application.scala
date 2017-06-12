package controllers
import javax.inject.Singleton

import play.api.libs.json.Json
import play.api.mvc._

/**
  * Application endpoint. Mainly used to check if the server is online.
  */
@Singleton
class Application extends Controller {

  /**
    * @return the state of the server.
    */
  def index = Action {
    Ok(Json.obj(
      "state" -> "Yea i'm up",
      "version" -> "v 1.0",
      "frontEnd" -> "https://akessonhenrik.github.io/Scalol-frontend/signup.html"
    ))
  }
}