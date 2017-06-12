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
  * This is the user endpoint. Everything related to users and auth is managed here.
  */
@Singleton
class UserEndpoint @Inject()(userDAO: UserService, PostDAO: PostService) extends Controller {

  import models.User.userReads
  import models.CompleteUserView.cUserWrite
  import models.UserAuth.UserAuthReads

  /**
    * Adds a user.
    *
    * @return 400 if the body is wrong or incomplete or at unexpected error
    *         200 otherwise
    */
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

  /**
    * Finds a user by it's username.
    *
    * @param username the username to find
    * @return 404 if the target user does not exists
    *         200 otherwise
    */
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

  /**
    * Finds a user by it's username.
    *
    * @param user_id the username to find
    * @return 404 if the target user does not exists
    *         200 otherwise
    */
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

  /**
    * Applies modifications to a user.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 if the body is wrong or incomplete or at unexpected error
    *         200 otherwise
    */
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

  /**
    * delete this user.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 at unexpected error
    *         200 otherwise
    */
  def deleteUser = UserAction.async { implicit request =>
    userDAO.delete(request.user.id.get).map(_ => Ok(Json.obj("status" -> "deleted")))
      .recover { case cause => BadRequest(Json.obj("reason" -> cause.getMessage)) }
  }

  /**
    * Generates a token for a valid tuple of username/password.
    *
    * @return 400 if the body is wrong or incomplete or at unexpected error
    *         403 if the username/password is wrong
    *         200 otherwise
    */
  def auth() = Action.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[UserAuth]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpU => {
        userDAO.findByUserName(tmpU.username).map(user => user.password == tmpU.password.sha512.hex match {
          case true => {
            val uuid = java.util.UUID.randomUUID.toString
            userDAO.createSession(user.id.get, uuid)
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

  /**
    * Checks if the token is still valid
    * This require an authenticated user. See UserAction for more details.
    */
  def check() = UserAction { implicit request =>
    Ok(Json.obj("status" -> "ok"))
  }
}
