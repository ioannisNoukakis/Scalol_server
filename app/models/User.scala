package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._


import slick.driver.MySQLDriver.api._

/**
  * Created by lux on 16/04/2017.
  */

case class User(username: String, mail: String, password: String, id: Option[Long] = None, rank: Option[Int] = None)

object User {
  implicit val userReads = Json.reads[User]
  implicit val userWrite = Json.writes[User]
}

//noinspection TypeAnnotation
class UserTableDef(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.PrimaryKey)
  def mail = column[String]("mail")
  def password = column[String]("password")
  def rank = column[Int]("rank")

  def * = (username, mail, password, id.?, rank.?) <> ((User.apply _).tupled, User.unapply)
}