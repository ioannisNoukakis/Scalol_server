package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import play.api.mvc._
import models.{CompleteUserView, User, UserView}
import services.{PostService, UserService}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._

import scala.concurrent.Future
import com.roundeights.hasher.Implicits._

import scala.language.postfixOps
import pdi.jwt.{JwtAlgorithm, JwtJson}

/**
  * Created by lux on 16/04/2017.
  */
@Singleton
class UserEndpoint @Inject()(userDAO: UserService, PostDAO: PostService) extends Controller {
  import models.UserView.userReads
  import models.CompleteUserView.cUserWrite

  def addUser = Action.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[UserView]
    result.fold(
      errors => Future {BadRequest(JsError.toJson(errors))},
      tmpU => {
        val user : User = User(tmpU.username.get, tmpU.mail.get, tmpU.password.get.sha512.hex)
        userDAO.insert(user).map(u => {
          val uuid = java.util.UUID.randomUUID.toString
          userDAO.createSession(u.id.get, uuid).map(_ => ())
          Ok(Json.obj("token" ->  JwtJson.encode(Json.obj(("uuid", uuid)), "secret", JwtAlgorithm.HS512)))
        })
          .recover {case cause => BadRequest(Json.obj("cause" -> cause.getMessage))}
      }
    )
  }

  def findByUsername(username: String) = Action.async { implicit request =>
    userDAO.findByUserName(username).flatMap(user => {
      PostDAO.getUserPosts(user.id.get).map(posts =>{
        Ok(Json.toJson(CompleteUserView(UserView(Option{user.username}, Option {user.mail}, None ,user.id, user.rank),
        posts)))
      })
    })
      .recover {case cause => NotFound(Json.obj("reason" -> cause.getMessage))}
  }

  def patchUser = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[UserView]
    result.fold(
      errors => Future {BadRequest(JsError.toJson(errors))},
      tmpU => {
        userDAO.updateUser(request.userSession.user_id, tmpU.username.get, tmpU.mail.get, tmpU.password.get.sha512.hex)
          .map(_ => Ok(Json.obj("status"->"ok")))
          .recover {case cause => NotFound(Json.obj("reason" -> cause.getMessage))}
      }
    )
  }

  def deleteUser = UserAction.async { implicit request =>
    userDAO.deleteUser(request.userSession.user_id).map(_ => Ok(Json.obj("status" -> "deleted")))
      .recover {case cause => Forbidden(Json.obj("reason" -> cause.getMessage))}
  }

  def auth() = Action.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[UserView]
    result.fold(
      errors => Future {BadRequest(JsError.toJson(errors))},
      tmpU => tmpU.password match {
        case None => Future {BadRequest(Json.obj("cause" -> "Missing password"))}
        case _ => {
          userDAO.findByUserName(tmpU.username.get).map(user => user.password == tmpU.password.getOrElse("").sha512.hex match {
            case true => { //TODO MAKE THE KEY SECRECT
              val uuid = java.util.UUID.randomUUID.toString
              userDAO.createSession(user.id.get, uuid) //TODO IS VERIFICATIONS REALLY OKAY?
              Ok(Json.obj("token" -> JwtJson.encode(Json.obj("uuid" -> uuid), "secret", JwtAlgorithm.HS512)))
            }
            case false => Forbidden(Json.obj("cause" -> "Invalid password or username"))
          }).recover {case cause => BadRequest(Json.obj("reason" -> cause.getMessage))}
        }
      }
    )
  }
}
