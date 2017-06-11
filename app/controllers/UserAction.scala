package controllers

import java.sql.Date
import java.util.Calendar

import models.{User, UserSession, UserSesssionTableDef, UserTableDef}
import pdi.jwt.exceptions.JwtLengthException
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import scala.concurrent.{Await, Future}
/**
  * Created by lux on 19/05/2017.
  */

class AuthenticatedRequest[A](val user: User, val request: Request[A]) extends WrappedRequest[A](request)

object UserAction extends ActionBuilder[AuthenticatedRequest] {
  val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val userSessions = TableQuery[UserSesssionTableDef]
  val users: TableQuery[UserTableDef] = TableQuery[UserTableDef]


  override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    try {
      val session: JsObject = JwtJson.decodeJson(request.headers.get("auth").get, "secret", Seq(JwtAlgorithm.HS512)).get
      val tmp = db.run(userSessions.filter(u => u.session === (session \ "uuid").as[String]).result).map(dbObject => dbObject.head)
      val userSession = Await.result(tmp, scala.concurrent.duration.Duration.Inf)

      //is usersession valid?
      if( new Date(Calendar.getInstance().getTime.getTime).compareTo(userSession.expires)  > 0) {
        db.run(userSessions.filter(u => u.session === (session \ "uuid").as[String]).delete)
        Future { Results.Forbidden(Json.obj("cause" -> "Outdated auth. Please auth again.")) }
      }

      block(new AuthenticatedRequest(Await.result(db.run(users.filter(_.id === userSession.user_id).result.head),scala.concurrent.duration.Duration.Inf), request))
    } catch {
      case _: NoSuchElementException => Future { Results.BadRequest(Json.obj("cause" -> "Missing auth.")) }
      case _: JwtLengthException => Future { Results.Forbidden(Json.obj("cause" -> "Invalid auth.")) }
      case _: UnsupportedOperationException => Future { Results.Forbidden(Json.obj("cause" -> "Invalid auth.")) }
      case cause => println(cause); Future {Results.BadRequest(Json.obj("cause" -> cause.getMessage))}
    }
  }
}