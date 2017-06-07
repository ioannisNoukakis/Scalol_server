package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import models._
import pdi.jwt.{JwtAlgorithm, JwtJson}
import pdi.jwt.exceptions.JwtLengthException
import play.api.mvc._
import play.api.libs.streams._
import services.{AuthService, MessageService, UserService}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsObject, Json}

import scala.concurrent.{Await, Future}

/**
  * Created by durza9390 on 28.05.2017.
  */
@Singleton
class MessageEndpoint @Inject()(implicit MessageDAO: MessageService, UserDAO: UserService, authService: AuthService, system: ActorSystem, materializer: Materializer) extends Controller  {

  import models.MessageFrom.messageWrites
  import models.MessageTo.messageReads

  def addMessage(to_username: String) = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[MessageTo]
    result.fold(
      errors => Future {
        BadRequest(JsError.toJson(errors))
      },
      tmpM => {
        UserDAO.findByUserName(to_username).flatMap(u => {
          MessageDAO.isUserBlocked(request.userSession.user_id, u.id.get).flatMap(blocked => blocked match {
            case Some(x) => x.user_blocked match {
              case true => Future {Forbidden(Json.obj("cause" -> "This user has blocked you."))}
              case false => MessageDAO.insert(Message(tmpM.content, false, false, new Date(Calendar.getInstance().getTime().getTime), request.userSession.user_id, u.id.get, None))
                .map(_ => {
                  Ok(Json.obj("state" -> "ok"))
                })
            }
            case None => MessageDAO.insert(Message(tmpM.content, false, false, new Date(Calendar.getInstance().getTime().getTime), request.userSession.user_id, u.id.get, None))
              .map(_ => Ok(Json.obj("state" -> "ok")))
          })
        })
          .recover { case cause => NotFound(Json.obj("reason" -> cause.getMessage)) }
      }
    )
  }

  def getMessages(from_username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(from_username).flatMap(u => {
      MessageDAO.getLastMessages(request.userSession.user_id, u.id.get).map(r => Ok(Json.toJson(r.map(m => MessageFrom(u.username, m.content, m.date.toString, m.viewed, m.user_blocked)))))
    })
      .recover { case cause => NotFound(Json.obj("reason" -> cause.getMessage)) }
  }

  def blockUser(username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(username).flatMap(u => {
      MessageDAO.updateBlockFromUser(request.userSession.user_id, u.id.get, true).map( _ => {
        Ok(Json.obj("status" -> "user blocked."))
      })
    })
      .recover { case cause => NotFound(Json.obj("reason" -> cause.getMessage)) }
  }

  def unblockUser(username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(username).flatMap(u => {
      MessageDAO.updateBlockFromUser(request.userSession.user_id, u.id.get, false).map( _ => {
        Ok(Json.obj("status" -> "user unblocked."))
      })
    })
      .recover { case cause => NotFound(Json.obj("reason" -> cause.getMessage)) }
  }

  def markAsRead(to_username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(to_username).flatMap(u => {
      MessageDAO.updateViewed(request.userSession.user_id, u.id.get).map( _ => {
        Ok(Json.obj("status" -> "Messages marked as read."))
      })
    })
      .recover { case cause => NotFound(Json.obj("reason" -> cause.getMessage)) }
  }

  def chat = WebSocket.accept[String, String] { request =>
    val t = authService.verifyToken(request.headers.get("auth").get).map(u => {
      ActorFlow.actorRef(out => ChatRoom.props(out, u.username, request.headers.get("to").get))
    })
    Await.result(t, scala.concurrent.duration.Duration.Inf)
  }
}