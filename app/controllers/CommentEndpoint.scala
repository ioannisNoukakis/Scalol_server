package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import models.Comment
import play.api.libs.json.{JsError, Json}
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
      errors => Future {BadRequest(JsError.toJson(errors))},
      tmpC => {
        UserDAO.findById(request.userSession.user_id).flatMap(u => {
          CommentDAO.insert(new Comment(tmpC.post_id, tmpC.content, Option { u.username }, None)).map(newPost => Ok(Json.obj("status:" -> "OK")))
            .recover { case cause => BadRequest(Json.obj("cause" -> cause.getMessage)) }
        })
      }
    )
  }

  def getComments(post_id: Long) = Action.async { implicit request =>
    CommentDAO.getByPostId(post_id).map(result => Ok(Json.toJson(result)))
      .recover{case cause => BadRequest(Json.obj("cause" -> cause.getMessage))}
  }
}
