package models

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api._

/**
  * Created by lux on 17/05/2017.
  */
case class Post(title: String,
                image_path: String,
                score: Long,
                nsfw: Boolean,
                owner_id: Long,
                id: Option[Long])
            extends BaseModel(id)

case class PostView(title: String,
                image_path: String,
                score: Long,
                nsfw: Boolean,
                owner: String,
                id:Long)

object Post {
  implicit val postReads = Json.reads[Post]
  implicit val postWrites = Json.writes[Post]
}

object PostView {
  implicit val postReads = Json.reads[PostView]
  implicit val postWrites = Json.writes[PostView]
}

case class PostPartial(title: String, image_path: String, nsfw: Boolean)

object PostPartial {
  implicit val postViewReads = Json.reads[PostPartial]
  implicit val postViewWrites = Json.writes[PostPartial]
}

case class UserUpvotes(inc: Boolean,
                      post_id: Long,
                      user_id: Long)

class PostTableDef(tag: Tag) extends BaseModelTableDef[Post](tag, "post") {
  override def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def image_path = column[String]("image_path")
  def score = column[Long]("score")
  def nsfw = column[Boolean]("nsfw")
  def owner_id = column[Long]("owner_id")
  def user = foreignKey("user_post_fk", owner_id, TableQuery[UserTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (title, image_path, score, nsfw, owner_id, id.?) <> ((Post.apply _).tupled, Post.unapply)
}

class UserUpvotesTableDef(tag: Tag) extends Table[UserUpvotes](tag, "user_upvote") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def inc = column[Boolean]("inc")
  def user_id = column[Long]("user_id")
  def post_id = column[Long]("post_id")
  def user = foreignKey("user_session_user_fk", user_id, TableQuery[UserTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (inc, post_id, user_id) <> ((UserUpvotes.apply _).tupled, UserUpvotes.unapply)
}