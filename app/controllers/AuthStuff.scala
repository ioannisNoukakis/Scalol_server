package controllers

import javassist.tools.web.BadHttpRequest

import models.{UserSession, UserSesssionTableDef}
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

case class UuidWrapper(uuid:String)

object UserAction extends ActionBuilder[AuthenticatedRequest] {
  val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val users = TableQuery[UserSesssionTableDef]

  override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    try {
      val session: JsObject = JwtJson.decodeJson(request.headers.get("auth").get, "secret", Seq(JwtAlgorithm.HS512)).get
      val tmp = db.run(users.filter(u => u.session === (session \ "uuid").as[String]).result).map(dbObject => dbObject.head)
      val userSession = Await.result(tmp, scala.concurrent.duration.Duration.Inf)

      block(new AuthenticatedRequest(userSession, request))
    } catch {
      case _ => Future {Results.BadRequest(Json.obj("cause" -> "Invalid auth"))}
    }
  }
}