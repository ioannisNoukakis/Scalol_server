package controllers

import javax.inject.Singleton

import com.google.inject.Inject
import models.{Post, PostPartial, PostView}
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}
import services.{PostService, UserService}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.{Await, Future}

/**
  * Created by lux on 22/05/2017.
  */
@Singleton
class PostEndpoint @Inject()(PostDAO: PostService, UserDAO : UserService) extends Controller {

  import models.PostPartial.postViewReads
  import models.PostView.postWrites

  val MAX_UPLOAD_SIZE = 5000000 //Byte
  val HOSTNAME_IMAGE = "image/"

  def addPost = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[PostPartial]
    result.fold(
      _ => Future {
        BadRequest(Json.obj("cause" -> "Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)"))
      },
      tmpP => {
        PostDAO.addPost(new Post(tmpP.title, tmpP.image_path, 0, tmpP.nsfw, request.user.id.get, None)).map(newPost =>
          Ok(Json.obj("location:" -> (utils.ConfConf.serverAdress + "posts/" + newPost.id.get), "owner: " -> request.user.username)))
        .recover { case cause => BadRequest(Json.obj("cause" -> cause.getMessage)) }
      }
    )
  }

  def convertHelper(post: Post): PostView ={
    val user = Await.result(UserDAO.findById(post.owner_id), scala.concurrent.duration.Duration.Inf)
    PostView(post.title, post.image_path, post.score, post.nsfw, user.username, post.id.get)
  }

  def getPosts(offset: Option[Long], number: Option[Long]) = Action.async { implicit request =>
    PostDAO.all(offset.getOrElse(-1), number.getOrElse(100)).map(result => Ok(Json.toJson(result.map(post => convertHelper(post)))))
  }

  def findPostById(post_id: Long) = Action.async { implicit request =>
    PostDAO.findById(post_id).map(post => Ok(Json.toJson(convertHelper(post))))
      .recover { case cause => NotFound(Json.obj("cause" -> "Post not found")) }
  }

  def upvote(post_id: Long) = UserAction.async { implicit request =>
    PostDAO.findById(post_id).flatMap(_ => {
      PostDAO.updateUserAndPostUpvotesOrFalse(post_id, request.user.id.get, 1).flatMap {
        case (true, true) => PostDAO.modifyScore(post_id, 2).map(_ => Ok(Json.obj("status" -> "ok")))
        case (true, false) => PostDAO.modifyScore(post_id, 1).map(_ => Ok(Json.obj("status" -> "ok")))
        case (false, false) => Future {
          Forbidden(Json.obj("cause" -> "You have already upvoted this post."))
        }
      }
    })
      .recover {
        case _: UnsupportedOperationException => NotFound(Json.obj("cause" -> "Nonexistent post."))
        case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
      }
  }

  def downvote(post_id: Long) = UserAction.async { implicit request =>
    PostDAO.findById(post_id).flatMap(_ => {
      PostDAO.updateUserAndPostUpvotesOrFalse(post_id, request.user.id.get, -1).flatMap(a => a match {
        case (true, true) => PostDAO.modifyScore(post_id, -2).map(_ => Ok(Json.obj("status" -> "ok")))
        case (true, false) => PostDAO.modifyScore(post_id, -1).map(_ => Ok(Json.obj("status" -> "ok")))
        case (false, false) => Future {
          Forbidden(Json.obj("cause" -> "You have already upvoted this post."))
        }
      })
    })
      .recover {
        case _: UnsupportedOperationException => NotFound(Json.obj("cause" -> "Nonexistent post."))
        case cause => BadRequest(Json.obj("cause" -> cause.getMessage))
      }
  }

  def uploadPic = UserAction.async(parse.multipartFormData) { request =>
    if (request.request.headers.get("Content-Length").get.toInt > MAX_UPLOAD_SIZE)
      Future {
        BadRequest(Json.obj("status" -> "File is too big (Max size: 5'000'000 Bytes)"))
      }
    else {
      request.body.file("picture").map { picture =>
        import java.io.File
        var extention = ""
        picture.contentType match {
          case None => Future{ BadRequest(Json.obj("cause" -> "You need to specify a content type.")) }
          case Some("image/jpeg") => extention = ".jpg"
          case Some("image/png") => extention = ".png"
          case Some("image/gif") => extention = ".gif"
          case _ => Future{ BadRequest(Json.obj("cause" -> "This content type is not supported. Only jpeg, png or gif are allowed.")) }
        }
        val filename: String = java.util.UUID.randomUUID.toString + System.currentTimeMillis().toString + extention
        new File(s"/scalolUploads").mkdir()
        picture.ref.moveTo(new File(s"/scalolUploads/$filename"))
        Future {
          Ok(Json.obj("location" -> (utils.ConfConf.serverAdress + HOSTNAME_IMAGE + filename)))
        }
      }.getOrElse {
        Future {
          BadRequest(Json.obj("cause" -> "something went wrong. Did you set the key of your content to 'picture'?"))
        }
      }
    }
  }
}
