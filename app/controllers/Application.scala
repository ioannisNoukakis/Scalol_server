package controllers
import javax.inject.Singleton

import play.api.libs.json.Json
import play.api.mvc._

@Singleton
class Application extends Controller {

  def index = Action {
    Ok(Json.obj(
      "state" -> "Yea i'm up",
      "version" -> "v 1.0"
    ))
  }
}