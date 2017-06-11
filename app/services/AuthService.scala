package services

import java.sql.Date
import java.util.Calendar
import javax.inject.Inject

import controllers.UserAction.{db, userSessions, users}
import models.{User, UserSesssionTableDef, UserTableDef}
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import scala.concurrent._
/**
  * Created by durza9390 on 07.06.2017.
  */
class AuthService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, UserDAO: UserService) extends HasDatabaseConfigProvider[JdbcProfile]{

  val users: TableQuery[UserTableDef] = TableQuery[UserTableDef]
  val us = TableQuery[UserSesssionTableDef]

  def verifyToken(token: String): Future[User] = {
    val session: JsObject = JwtJson.decodeJson(token, "secret", Seq(JwtAlgorithm.HS512)).get
    val tmp = db.run(userSessions.filter(u => u.session === (session \ "uuid").as[String]).result).map(dbObject => dbObject.head)
    val userSession = Await.result(tmp, scala.concurrent.duration.Duration.Inf)

    //is usersession valid?
    if( new Date(Calendar.getInstance().getTime.getTime).compareTo(userSession.expires)  > 0) {
      db.run(userSessions.filter(u => u.session === (session \ "uuid").as[String]).delete)
      return null
    }
    db.run(users.filter(_.id === userSession.user_id).result.head)
  }
}