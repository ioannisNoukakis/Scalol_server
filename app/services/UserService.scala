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

  val Users = TableQuery[UserTableDef]
  val us = TableQuery[UserSesssionTableDef]

  def newDate() = new Date(Calendar.getInstance().getTime().getTime + 604800000)

  def all(): Future[Seq[User]] = db.run(Users.take(100).result)

  def insert(u: User)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(Users += u).map( _ => ())
  }

  def findByUserName(username: String)(implicit ec: ExecutionContext): Future[User] = {
    db.run(Users.filter(u => u.username === username).result).map(dbObject => dbObject.head)
  }

  def updateSession(user_id: Long, newSession: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val q = for { c <- us if c.user_id === user_id } yield c.expires
    db.run(q.update(newDate())).map( _ => ())
  }

  def createNewSession(user_id: Long, newSession: String)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(us += UserSession(java.util.UUID.randomUUID.toString, newDate(), user_id)).map( _ => ())
  }

  def createOrUpdateSession(username: String, newSession: String)(implicit ec: ExecutionContext): Future[Unit] = {
    findByUserName(username).map(user => user)
  }
}

/**
  *
  */