package services

/**
  * Created by lux on 16/04/2017.
  */
import java.sql.Date
import java.util.Calendar

import models.{User, UserSession, UserSesssionTableDef, UserTableDef}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent._


class UserService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  val users = TableQuery[UserTableDef]
  val us = TableQuery[UserSesssionTableDef]

  def newDate() = new Date(Calendar.getInstance().getTime().getTime + 604800000)

  def all(): Future[Seq[User]] = db.run(users.take(100).result)

  def insert(user: User)(implicit ec: ExecutionContext): Future[User] = {
    val insertQuery = users returning users.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += user
    db.run(action)
  }

  def findByUserName(username: String)(implicit ec: ExecutionContext): Future[User] = {
    db.run(users.filter(u => u.username === username).result).map(dbObject => dbObject.head)
  }

  def createSession(user_id: Long, newSession: String)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(us += UserSession(newSession, newDate(), user_id)).map( _ => ())
  }

  def userHasSession(user_id: Long)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(users.filter(u => u.id === user_id).result).map(dbObject => dbObject.head)
  }
}