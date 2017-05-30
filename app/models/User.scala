package models

import java.sql.Date

import play.api.libs.json._
import slick.driver.MySQLDriver.api._

/**
  * Created by lux on 16/04/2017.
  */

case class User(username: String,
                mail: String,
                password: String,
                id: Option[Long] = None,
                rank: Option[Int] = Option {0})

case class CompleteUserView(user: User, posts: Seq[Post])

case class UserAuth(username: String,
                    password: String)

object User {
  implicit val userReads = Json.reads[User]
  implicit val userWrite = Json.writes[User]
}

object CompleteUserView {
  implicit val cUserReads = Json.reads[CompleteUserView]
  implicit val cUserWrite = Json.writes[CompleteUserView]
}

object UserAuth {
  implicit val UserAuthReads = Json.reads[UserAuth]
  implicit val UserAuthWrite = Json.writes[UserAuth]
}

case class UserSession(session: String,
                      expires: Date,
                       user_id: Long)

//noinspection TypeAnnotation
class UserTableDef(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.PrimaryKey)
  def mail = column[String]("mail")
  def password = column[String]("password")
  def rank = column[Int]("rank")

  def * = (username, mail, password, id.?, rank.?) <> ((User.apply _).tupled, User.unapply)
}

//noinspection TypeAnnotation
class UserSesssionTableDef(tag: Tag) extends Table[UserSession](tag, "user_session") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def session = column[String]("session", O.PrimaryKey)
  def expires = column[Date]("expires")
  def user_id = column[Long]("user_id")
  def user = foreignKey("user_session_user_fk", user_id, TableQuery[UserTableDef])(_.id, onDelete=ForeignKeyAction.Cascade)

  def * = (session, expires, user_id) <> ((UserSession.apply _).tupled, UserSession.unapply)
}