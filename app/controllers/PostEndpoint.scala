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
  * This is the post endpoint. Here is managed everything related to post and image upload.
  */
@Singleton
class PostEndpoint @Inject()(PostDAO: PostService, UserDAO : UserService) extends Controller {

  import models.PostPartial.postViewReads
  import models.PostView.postWrites

  /**
    * Adds a post to the server.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 if the body is wrong or incomplete or if an unknown error appears.
    *         200 otherwise.
    */
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

  /**
    * Helper for post write. System requires to have the username post owner instead of their id.
    * This is a temporary solution and soon the username will be added to the post table as well.
    *
    * @param post the post to be converted.
    * @return a PostView
    */
  def convertHelper(post: Post): PostView ={
    val user = Await.result(UserDAO.findById(post.owner_id), scala.concurrent.duration.Duration.Inf)
    PostView(post.title, post.image_path, post.score, post.nsfw, user.username, post.id.get)
  }

  /**
    * Get a list of posts. If no querystring parameter, will return the 100 last posts.
    *
    * @param offset: the post id from where to take posts.
    * @param number: the number of posts to take.
    * @return a list of posts to the client.
    */
  def getPosts(offset: Option[Long], number: Option[Long]) = Action.async { implicit request =>
    PostDAO.all(offset.getOrElse(-1), number.getOrElse(100)).map(result => Ok(Json.toJson(result.map(post => convertHelper(post)))))
  }

  /**
    * Get a post by id.
    *
    * @param post_id the post to be retrived.
    * @return 404 if the post was not found
    *         200 otherwise
    */
  def findPostById(post_id: Long) = Action.async { implicit request =>
    PostDAO.findById(post_id).map(post => Ok(Json.toJson(convertHelper(post))))
      .recover { case cause => NotFound(Json.obj("cause" -> "Post not found")) }
  }

  /**
    * Upvotes a post.
    * This require an authenticated user. See UserAction for more details.
    *
    * @param post_id the post to be upvoted.
    * @return 400 on unexpected errors.
    *         403 if you have already upvoted the post
    *         200 otherwise
    */
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

  /**
    * Downvotes a post.
    * This require an authenticated user. See UserAction for more details.
    *
    * @param post_id the post to be downvoted.
    * @return 400 on unexpected errors.
    *         403 if you have already downvoted the post
    *         200 otherwise
    */
  def downvote(post_id: Long) = UserAction.async { implicit request =>
    PostDAO.findById(post_id).flatMap(_ => {
      PostDAO.updateUserAndPostUpvotesOrFalse(post_id, request.user.id.get, -1).flatMap {
        case (true, true) => PostDAO.modifyScore(post_id, -2).map(_ => Ok(Json.obj("status" -> "ok")))
        case (true, false) => PostDAO.modifyScore(post_id, -1).map(_ => Ok(Json.obj("status" -> "ok")))
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

  /**
    * Uploads a picture (PNG/JPEG/GIF) to the server.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 if the file is greater than 5MB or on unexpected error.
    *         200 otherwise
    */
  def uploadPic = UserAction.async(parse.multipartFormData) { request =>
    if (request.request.headers.get("Content-Length").get.toInt > utils.ConfConf.MAX_UPLOAD_SIZE)
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
          Ok(Json.obj("location" -> (utils.ConfConf.serverAdress + utils.ConfConf.HOSTNAME_IMAGE + filename)))
        }
      }.getOrElse {
        Future {
          BadRequest(Json.obj("cause" -> "something went wrong. Did you set the key of your content to 'picture'?"))
        }
      }
    }
  }
}
