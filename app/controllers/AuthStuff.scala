package controllers

import java.sql.Date
import java.util.Calendar

import models.{UserSession, UserSesssionTableDef}
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

class AuthenticatedRequest[A](val userSession: UserSession, val request: Request[A]) extends WrappedRequest[A](request)

object UserAction extends ActionBuilder[AuthenticatedRequest] {
  val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val users = TableQuery[UserSesssionTableDef]

  override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    try {
      val session: JsObject = JwtJson.decodeJson(request.headers.get("auth").get, "secret", Seq(JwtAlgorithm.HS512)).get
      val tmp = db.run(users.filter(u => u.session === (session \ "uuid").as[String]).result).map(dbObject => dbObject.head)
      val userSession = Await.result(tmp, scala.concurrent.duration.Duration.Inf)

      //is usersession valid?
      if( new Date(Calendar.getInstance().getTime().getTime).compareTo(userSession.expires)  > 0) {
        db.run(users.filter(u => u.session === (session \ "uuid").as[String]).delete)
        Future { Results.Forbidden(Json.obj("cause" -> "Outdated auth. Please auth again.")) }
      }

      block(new AuthenticatedRequest(userSession, request))
    } catch {
      case _: NoSuchElementException => Future { Results.BadRequest(Json.obj("cause" -> "Missing auth.")) }
      case _: JwtLengthException => Future { Results.Forbidden(Json.obj("cause" -> "Invalid auth.")) }
      case _: UnsupportedOperationException => Future { Results.Forbidden(Json.obj("cause" -> "Invalid auth.")) }
      case cause => println(cause); Future {Results.BadRequest(Json.obj("cause" -> cause.getMessage))}
    }
  }
}
/*
object AuthWebSocket extends WebSocket {

  override def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, _]]] = {
    val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
    val usersSession = TableQuery[UserSesssionTableDef]
    val users = TableQuery[UserTableDef]

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
  }
}*/