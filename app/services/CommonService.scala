package services

import java.sql.Date
import java.util.Calendar

import models._
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent._

/**
  * Created by lux on 06/06/2017.
  */
class CommonService[T, TableDef] @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  val table = TableQuery[TableDef]

  def all(): Future[Seq[T]] = db.run(table.take(100).result).map(_.map(_.asInstanceOf[T]))

  def insert(user: User)(implicit ec: ExecutionContext): Future[User] = {
    val insertQuery = table returning table.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += user
    db.run(action)
  }

  def findByUserName(username: String)(implicit ec: ExecutionContext): Future[User] = {
    db.run(table.filter(u => u.username === username).result).map(dbObject => dbObject.head)
  }

  def findById(user_id: Long)(implicit ec: ExecutionContext): Future[User] = {
    db.run(table.filter(u => u.id === user_id).result).map(dbObject => dbObject.head)
  }

  def createSession(user_id: Long, newSession: String)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(us += UserSession(newSession, newDate(), user_id)).map( _ => ())
  }

  def userHasSession(user_id: Long)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(table.filter(u => u.id === user_id).result).map(dbObject => dbObject.head)
  }

  def updateUser(user_id: Long, newUsername: String, newMail: String, newPassword: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val q = for {u <- table if u.id === user_id} yield (u.username, u.mail, u.password)
    val updateAction = q.update((newUsername, newMail, newPassword))
    db.run(updateAction).map(_ => ())
  }

  def deleteUser(user_id: Long)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(table.filter(_.id === user_id).delete).map(_=>())
  }
}
