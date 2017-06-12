package models

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api._

/**
  * Comments model
  */
case class Comment(post_id: Long, content: String, username: Option[String], id: Option[Long])
  extends BaseModel(id)

object Comment {
  implicit val commentReads = Json.reads[Comment]
  implicit val commentWrites = Json.writes[Comment]
}

class CommentTableDef(tag: Tag) extends BaseModelTableDef[Comment](tag, "comment") {
  override def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def post_id = column[Long]("post_id")
  def content = column[String]("content")
  def post = foreignKey("comment_post_fk", post_id, TableQuery[PostTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (post_id, content, username.?, id.?) <> ((Comment.apply _).tupled, Comment.unapply)
}