package controllers
import com.google.inject.Inject
import play.api.mvc._
import models.User
import services.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by lux on 16/04/2017.
  */
class UserEndpoint @Inject()(userDAO: UserService) extends Controller {
  import User.userReads
  import User.userWrite

  def index = Action.async {
    userDAO.all().map(result => Ok(Json.toJson(result)))
  }

  def addUser = Action.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[User]
    result.fold(
      errors => Future {BadRequest(JsError.toJson(errors))},
      user => {
        userDAO.insert(user).map(_ => Ok(Json.obj("state" -> "ok"))).
          recover {case cause => BadRequest(Json.obj("reason" -> cause.getMessage))}
      }
    )
  }
}

/**
  * Ok(Json.obj("state" -> "ok"))
  * BadRequest(Json.obj("reason" -> exception.getMessage))
  */
