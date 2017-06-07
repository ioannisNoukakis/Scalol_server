package services

import javax.inject.Inject

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

  def verifyToken (token: String): Future[User] = {
    val session: JsObject = JwtJson.decodeJson(token, "secret", Seq(JwtAlgorithm.HS512)).get
    db.run(us.filter(u => u.session === (session \ "uuid").as[String]).result)
      .map(dbObject => dbObject.head)
      .flatMap(userSess => {
        db.run(users.filter(_.id === userSess.user_id).result).map(dbObject => dbObject.head)
      })
  }
}
/*
try {
      val session: JsObject = JwtJson.decodeJson(request.headers.get("auth").get, "secret", Seq(JwtAlgorithm.HS512)).get
      val tmp = db.run(usersSession.filter(u => u.session === (session \ "uuid").as[String]).result).map(dbObject => dbObject.head)
      val userSession = Await.result(tmp, scala.concurrent.duration.Duration.Inf)

      //is usersession valid?
      if( new Date(Calendar.getInstance().getTime().getTime).compareTo(userSession.expires)  > 0) {
        db.run(usersSession.filter(u => u.session === (session \ "uuid").as[String]).delete)
        return Future {Left(Results.Forbidden(Json.obj("cause" -> "Outdated auth. Please auth again.")))}
      }

      val user = Await.result(db.run(users.filter(_.id === userSession.user_id).result).map(dbObject => dbObject.head), scala.concurrent.duration.Duration.Inf)

      request.headers.add(("username", user.username))
      return super.apply(request)

    } catch {
      case _: NoSuchElementException => Future { Left(Results.BadRequest(Json.obj("cause" -> "Missing auth.")))}
      case _: JwtLengthException => Future { Left(Results.Forbidden(Json.obj("cause" -> "Invalid auth.")))}
      case _: UnsupportedOperationException => Future { Left(Results.Forbidden(Json.obj("cause" -> "Invalid auth.")))}
      case cause => println(cause); Future {Left(Results.BadRequest(Json.obj("cause" -> cause.getMessage)))}
    }
 */
