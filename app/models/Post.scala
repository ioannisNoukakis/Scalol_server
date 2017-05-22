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

object Post {
  implicit val postReads = Json.reads[Post]
  implicit val postWrites = Json.writes[Post]
}

case class PostView (title: String, image_path: String, nsfw: Boolean)

object PostView {
  implicit val postViewReads = Json.reads[PostView]
  implicit val postViewWrites = Json.writes[PostView]
}

class PostTableDef(tag: Tag) extends Table[Post](tag, "post") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def image_path = column[String]("image_path")
  def score = column[Long]("score")
  def nsfw = column[Boolean]("nsfw")
  def owner_id = column[Long]("owner_id")
  def user = foreignKey("user_post_fk", owner_id, TableQuery[UserTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (title, image_path, score, nsfw, owner_id, id.?) <> ((Post.apply _).tupled, Post.unapply)
}