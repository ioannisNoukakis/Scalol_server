package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import play.api.mvc._
import models.{CompleteUserView, User, UserAuth}
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

  import models.User.userReads
  import models.CompleteUserView.cUserWrite
  import models.UserAuth.UserAuthReads

  def addUser = Action.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[User]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpU => {
        //FIXME WE NEED A SALT TABLE / ROW ALONG WITH THE PASSWORD
        val user: User = User(tmpU.username, tmpU.mail, tmpU.password.sha512.hex, None, None)
        userDAO.addUser(user).map(u => {
          val uuid = java.util.UUID.randomUUID.toString
          userDAO.createSession(u.id.get, uuid).map(_ => ())
          Ok(Json.obj("token" -> JwtJson.encode(Json.obj(("uuid", uuid)), utils.ConfConf.serverSecret, JwtAlgorithm.HS512)))
        })
          .recover {
            case _:MySQLIntegrityConstraintViolationException => Conflict(Json.obj("cause" -> "Username already taken."))
            case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
          }
      }
    )
  }

  def findByUsername(username: String) = Action.async { implicit request =>
    userDAO.findByUserName(username).flatMap(user => {
      PostDAO.getUserPosts(user.id.get).map(posts => {
        Ok(Json.toJson(CompleteUserView(User(
          user.username
          ,
          user.mail
          , null, user.id, user.rank),
          posts)))
      })
    })
      .recover { case cause => NotFound(Json.obj("cause" -> "The following user does not exists.")) }
  }

  def findById(user_id: Long) = Action.async { implicit request =>
    userDAO.findById(user_id).flatMap(user => {
      PostDAO.getUserPosts(user.id.get).map(posts => {
        Ok(Json.toJson(CompleteUserView(User(
          user.username
          ,
          user.mail
          , null, user.id, user.rank),
          posts)))
      })
    })
      .recover { case cause => NotFound(Json.obj("cause" -> "The following user does not exists.")) }
  }

  def patchUser = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[User]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpU => {
        userDAO.update(request.user.id.get, User(tmpU.username, tmpU.mail, tmpU.password.sha512.hex, Some(request.user.id.get), Some(0)))
          .map(_ => Ok(Json.obj("status" -> "ok")))
          .recover {
            case _: MySQLIntegrityConstraintViolationException => Conflict(Json.obj("cause" -> "Username already taken."))
            case cause => cause.printStackTrace(); BadRequest(Json.obj("reason" -> cause.getMessage))
          }
      }
    )
  }

  def deleteUser = UserAction.async { implicit request =>
    userDAO.delete(request.user.id.get).map(_ => Ok(Json.obj("status" -> "deleted")))
      .recover { case cause => Forbidden(Json.obj("reason" -> cause.getMessage)) }
  }

  def auth() = Action.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[UserAuth]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpU => {
        userDAO.findByUserName(tmpU.username).map(user => user.password == tmpU.password.sha512.hex match {
          case true => { //TODO MAKE THE KEY SECRECT
            val uuid = java.util.UUID.randomUUID.toString
            userDAO.createSession(user.id.get, uuid) //TODO IS VERIFICATIONS REALLY OKAY?
            Ok(Json.obj("token" -> JwtJson.encode(Json.obj("uuid" -> uuid), utils.ConfConf.serverSecret, JwtAlgorithm.HS512)))
          }
          case false => Forbidden(Json.obj("cause" -> "Invalid password or username"))
        }).recover {
          case _: UnsupportedOperationException => Forbidden(Json.obj("reason" -> "Invalid password or username"))
          case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
        }
      }
    )
  }

  def check() = UserAction { implicit request =>
    Ok(Json.obj("status" -> "ok"))
  }
}
