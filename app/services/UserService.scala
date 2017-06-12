package services

import java.sql.Date
import java.util.Calendar

import models._
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent._

/**
  * User service
  */
class UserService @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[User, UserTableDef] {

  override protected val table: TableQuery[UserTableDef] = TableQuery[UserTableDef]
  val us = TableQuery[UserSesssionTableDef]

  /**
    * Helper to define a new date + one week
    * @return the new date
    */
  def newDate() = new Date(Calendar.getInstance().getTime.getTime + 604800000)

  /**
    * Adds a user.
    *
    * @param user the user to be added
    * @return the user with it's DB id
    */
  def addUser(user: User): Future[User] = {
    val insertQuery = table returning table.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += user
    db.run(action)
  }

  /**
    * Finds a user by it's username.
    *
    * @param username the username
    * @return the user.
    */
  def findByUserName(username: String): Future[User] = {
    db.run(table.filter(_.username === username).result).map(dbObject => dbObject.head)
  }

  /**
    * Creates a new session for the user.
    *
    * @param user_id the user id
    * @param newSession the new uuid
    * @return a futur of unit
    */
  def createSession(user_id: Long, newSession: String): Future[Unit] = {
    db.run(us += UserSession(newSession, newDate(), user_id)).map( _ => ())
  }
}