package controllers
import play.api.libs.json.{Json}

import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(Json.obj(
      "state" -> "Yea i'm up",
      "version" -> "v 1.0"
    ))
  }
}