package models

import java.sql.Date

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api._

/**
  * Created by durza9390 on 28.05.2017.
  */
case class Message(content: String,
                   viewed: Boolean,
                   user_blocked: Boolean,
                   date: Date,
                   first_id: Long,
                   second_id: Long,
                   id: Option[Long])

case class MessageFrom(from: String,
                       content: String,
                       date: String,
                       viewed: Boolean,
                       user_blocked: Boolean)

case class MessageTo(content: String)

object MessageFrom {
  implicit val messageWrites = Json.writes[MessageFrom]
}

object MessageTo {
  implicit val messageReads = Json.reads[MessageTo]
}

class MessageTableDef(tag: Tag) extends Table[Message](tag, "message") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def content = column[String]("content")
  def viewed = column[Boolean]("viewed")
  def user_blocked = column[Boolean]("user_blocked")
  def date = column[Date]("date")
  def first_id = column[Long]("first_id")
  def second_id = column[Long]("second_id")
  def first = foreignKey("message1_user_fk", first_id, TableQuery[UserTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)
  def second = foreignKey("message2_user_fk", second_id, TableQuery[UserTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (content, viewed, user_blocked, date, first_id, second_id, id.?) <> ((Message.apply _).tupled, Message.unapply)
}