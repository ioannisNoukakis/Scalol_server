package services

/**
  * Created by lux on 16/04/2017.
  */
import models.{User, UserTableDef}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.Json

import scala.concurrent._
import scala.util.{Failure, Success}


class UserService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  val Users = TableQuery[UserTableDef]

  def all(): Future[Seq[User]] = db.run(Users.result)

  def insert(u: User)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(Users += u).map( _ => ())
  }
}
