package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import models.{Post, PostView, UserView}
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}
import services.PostService
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Created by lux on 22/05/2017.
  */
@Singleton
class PostEndpoint @Inject()(PostDAO: PostService) extends Controller {
  import models.PostView.postViewReads
  import models.Post.postWrites

  def addPost = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[PostView]
    result.fold(
      errors => Future {BadRequest(JsError.toJson(errors))},
      tmpP => {
        PostDAO.insert(new Post(tmpP.title, tmpP.image_path, 0, tmpP.nsfw, request.userSession.user_id, None)).map(newPost => Ok(Json.obj("location:" ->
          ("http://nixme.ddns.net:9000/posts/" + newPost.id.get))))
          .recover{case cause => BadRequest(Json.obj("cause" -> cause.getMessage))}
      }
    )
  }

  def getPosts = Action.async { implicit request =>
    PostDAO.all().map(result => Ok(Json.toJson(result.map(post => post))))
  }

  def upvote(post_id: Long) = UserAction.async { implicit request =>
    PostDAO.modifyScore(post_id, 1).map(_ => Ok(Json.obj("status" -> "ok")))
      .recover{case cause => BadRequest(Json.obj("cause" -> cause.getMessage))}
  }

  def downvote(post_id: Long) = UserAction.async { implicit request =>
    PostDAO.modifyScore(post_id, -1).map(_ => Ok(Json.obj("status" -> "ok")))
      .recover{case cause => BadRequest(Json.obj("cause" -> cause.getMessage))}
  }
}
