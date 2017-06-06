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
  * Created by durza9390 on 25.05.2017.
  */
@Singleton
class CommentEndpoint @Inject()(CommentDAO: CommentService, UserDAO: UserService) extends Controller {

  import models.Comment.commentReads
  import models.Comment.commentWrites

  def addComment = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[Comment]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpC => {
        UserDAO.findById(request.userSession.user_id).flatMap(u => {
          CommentDAO.insert(new Comment(tmpC.post_id, tmpC.content, Option {
            u.username
          }, None)).map(_ => Ok(Json.obj("status:" -> "OK")))
            .recover {
              case _:MySQLIntegrityConstraintViolationException => NotFound(Json.obj("cause" -> "Nonexistent post."))
              case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
            }
        })
      }
    )
  }

  def getComments(post_id: Long) = Action.async { implicit request =>
    CommentDAO.findById(post_id).map(result => Ok(Json.toJson(result)))
      .recover {
        case _:UnsupportedOperationException => NotFound(Json.obj("cause" -> "Nonexistent post"))
        case cause => print(cause.getClass); BadRequest(Json.obj("cause" -> cause.getMessage))
      }
  }
}
