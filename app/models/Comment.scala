package models

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api._

/**
  * Created by durza9390 on 25.05.2017.
  */
case class Comment(post_id: Long, content: String, username: Option[String], id: Option[Long])

object Comment {
  implicit val commentReads = Json.reads[Comment]
  implicit val commentWrites = Json.writes[Comment]
}

class CommentTableDef(tag: Tag) extends Table[Comment](tag, "comment") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def post_id = column[Long]("post_id")
  def content = column[String]("content")
  def post = foreignKey("comment_post_fk", post_id, TableQuery[PostTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (post_id, content, username.?, id.?) <> ((Comment.apply _).tupled, Comment.unapply)
}