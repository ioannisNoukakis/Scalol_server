package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import models.Comment
import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers, Controller}
import services.{CommentService, UserService}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * This is the comment endpoint. Everything related to comments is made here.
  */
@Singleton
class CommentEndpoint @Inject()(CommentDAO: CommentService, UserDAO: UserService) extends Controller {

  import models.Comment.commentReads
  import models.Comment.commentWrites

  /**
    * Adds a comment to a post.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 if the body is wrong or incomplete or if an unknown error appears.
    *         404 if the post to be commented does not exists.
    *         200 otherwise.
    */
  def addComment = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[Comment]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpC => {
        CommentDAO.insert(new Comment(tmpC.post_id, tmpC.content, Some(request.user.username), None)).map(_ => Ok(Json.obj("status:" -> "OK")))
          .recover {
            case _:MySQLIntegrityConstraintViolationException => NotFound(Json.obj("cause" -> "Nonexistent post."))
            case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
          }
      }
    )
  }

  /**
    * Gets the comments from a post.
    *
    * @return 400 if an unknown error appears.
    *         404 if the post to be commented does not exists.
    *         200 otherwise.
    */
  def getComments(post_id: Long) = Action.async { implicit request =>
    CommentDAO.findByPostId(post_id).map(result => Ok(Json.toJson(result)))
      .recover {
        case c:UnsupportedOperationException =>c.getMessage match{
          case "empty.head" => Ok("[]")
          case _ => NotFound(Json.obj("cause" -> "Nonexistent post"))
        }
        case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
      }
  }
}
