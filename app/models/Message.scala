package models

import java.sql.Date

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult

/**
  * Message model
  */
case class Message(content: String,
                   viewed: Boolean,
                   user_blocked: Boolean,
                   date: Date,
                   first_id: Long,
                   second_id: Long,
                   id: Option[Long])
            extends BaseModel(id)

case class MessageFrom(from: String,
                       to: String,
                       content: String,
                       date: String,
                       viewed: Boolean,
                       user_blocked: Boolean)

case class MessageTo(content: String)

case class MessageBox(from: String)

object Message {
  implicit val getMessageResult = GetResult(r => {
    val id = r.nextLong()
    val content = r.nextString()
    val viewed = r.nextBoolean()
    val user_blocked = r.nextBoolean()
    val date = r.nextDate()
    val first_id = r.nextLong()
    val second_id = r.nextLong()
    Message(content, viewed, user_blocked, date, first_id, second_id, Some(id))
  })
}

object MessageBox {
  implicit val messageBoxWrties = Json.writes[MessageBox]
}

object MessageFrom {
  implicit val messageWrites = Json.writes[MessageFrom]
}

object MessageTo {
  implicit val messageReads = Json.reads[MessageTo]
}

class MessageTableDef(tag: Tag) extends BaseModelTableDef[Message](tag, "message") {
  override def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
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