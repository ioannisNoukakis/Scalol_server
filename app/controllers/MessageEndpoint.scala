package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Singleton

import WS.{ChatActor, ErrorMessageActor, NotificationActor}
import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.Materializer
import com.google.inject.Inject
import models._
import pdi.jwt.exceptions.JwtLengthException
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, Json}
import play.api.libs.streams._
import play.api.mvc._
import services.{AuthService, MessageService, UserService}

import scala.concurrent.{Await, Future}

/**
  * The is the message endpoint. Both websocket and plain http chat are made here.
  */
@Singleton
class MessageEndpoint @Inject()(implicit MessageDAO: MessageService, UserDAO: UserService, authService: AuthService, system: ActorSystem, materializer: Materializer) extends Controller {

  import models.MessageBox.messageBoxWrties
  import models.MessageFrom.messageWrites
  import models.MessageTo.messageReads

  /**
    * Adds a message on a conversation or creates a conversation.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 if the body is wrong or incomplete.
    *         403 if the users how you wish to speak to has blocked you.
    *         404 if the requested user is not found.
    *         200 otherwise.
    */
  def addMessage(to_username: String) = UserAction.async(BodyParsers.parse.json) { implicit request =>
    val result = request.body.validate[MessageTo]
    result.fold(
      errors => Future {
        BadRequest(JsError.toJson(errors))
      },
      tmpM => {
        UserDAO.findByUserName(to_username).flatMap(u => {
          MessageDAO.isUserBlocked(request.user.id.get, u.id.get).flatMap {
            case Some(x) => x.user_blocked match {
              case true => Future {
                Forbidden(Json.obj("cause" -> "This user has blocked you."))
              }
              case false => MessageDAO.insert(Message(tmpM.content, false, false, new Date(Calendar.getInstance().getTime().getTime), request.user.id.get, u.id.get, None))
                .map(_ => {
                  NotificationActor.clients.filter(_.user == u).foreach(_.sendNotification(u.username + " wants to chat with you. Click here!"))
                  Ok(Json.obj("state" -> "ok"))
                })
            }
            case None => MessageDAO.insert(Message(tmpM.content, false, false, new Date(Calendar.getInstance().getTime().getTime), request.user.id.get, u.id.get, None))
              .map(_ => {
                NotificationActor.clients.filter(_.user == u).foreach(_.sendNotification(request.user.username +
                  " has sent you a message!"))
                Ok(Json.obj("state" -> "ok"))
              })
          }
        })
          .recover { case cause => NotFound(Json.obj("cause" -> "This user does not exists.")) }
      }
    )
  }

  /**
    * Get a summary of the users conversation.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 400 if an error occures.
    *         200 otherwise.
    */
  def getUserInbox() = UserAction.async { implicit request =>
    MessageDAO.getUserMailBox(request.user.id.get).map(p => Ok(Json.toJson(p.map(a =>{
      MessageBox(Await.result(a.first_id match {
        case i if i == request.user.id.get => UserDAO.findById(a.second_id).map(u => u.username)
        case _=> UserDAO.findById(a.first_id).map(u => u.username)
      }, scala.concurrent.duration.Duration.Inf))
    }).distinct)))
      .recover { case cause => BadRequest(Json.obj("cause" -> cause.getMessage)) }
  }

  /**
    * Get the history of messages with an other user.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 404 if the requested user is not found.
    *         200 otherwise.
    */
  def getMessagesFrom(from_username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(from_username).flatMap(u => {
      MessageDAO.getLastMessages(request.user.id.get, u.id.get).map(r => Ok(Json.toJson(r.map {
        case m if m.first_id == request.user.id.get => MessageFrom(request.user.username, u.username, m.content, m.date.toString, m.viewed, m.user_blocked)
        case m => MessageFrom(u.username, request.user.username, m.content, m.date.toString, m.viewed, m.user_blocked)
      })))
    })
      .recover { case cause => NotFound(Json.obj("cause" -> cause.getMessage)) }
  }

  /**
    * Blocks a user from the chat system. It also kills
    * the two ChatActor if any user were connected to the chat live system.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 404 if the requested user is not found.
    *         200 otherwise.
    */
  def blockUser(username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(username).flatMap(u => {
      MessageDAO.updateBlockFromUser(request.user.id.get, u.id.get, true).map(_ => {
        ChatActor.clients.filter(a => a.to == request.user && a.from == u)
                            .foreach(a => {
                              a.out ! "This user blocked you!"
                              a.self ! PoisonPill
                            })
        ChatActor.clients.filter(a => a.from == request.user && a.to == u)
          .foreach(a => {
            a.out ! "User successfully blocked!"
            a.self ! PoisonPill
          })
        Ok(Json.obj("status" -> "user blocked."))
      })
    })
      .recover { case cause => NotFound(Json.obj("cause" -> cause.getMessage)) }
  }

  /**
    * Unblock a user on the chat system.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 404 if the requested user is not found.
    *         200 otherwise.
    */
  def unblockUser(username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(username).flatMap(u => {
      MessageDAO.updateBlockFromUser(request.user.id.get, u.id.get, false).map(_ => {
        Ok(Json.obj("status" -> "user unblocked."))
      })
    })
      .recover { case cause => NotFound(Json.obj("cause" -> cause.getMessage)) }
  }

  /**
    * Marks a history of message with a user as read.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return 404 if the requested user is not found.
    *         200 otherwise.
    */
  def markAsRead(to_username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(to_username).flatMap(u => {
      MessageDAO.updateViewed(request.user.id.get, u.id.get).map(_ => {
        Ok(Json.obj("status" -> "Messages marked as read."))
      })
    })
      .recover { case cause => NotFound(Json.obj("cause" -> cause.getMessage)) }
  }

  /**
    * Helper function to notifiy the clients on the notify channel and create a flow.
    *
    * @param to: The destination user
    * @param from: The source user
    * @return a Flow[in, out, _]
    */
  def notifyAndCreateActor(to: User, from: User) = {
    NotificationActor.clients.filter(_.user == to).foreach(_.sendNotification(from.username + " is now online!"))
    Future {
      ActorFlow.actorRef(out => ChatActor.props(out, from, to))
    }
  }

  /**
    * Websocket endpoint to connect to the chat live system.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return a string response to the user.
    */
  def chat(token: Option[String], to: Option[String]) = WebSocket.accept[String, String] { request =>
    try {
      //verify token
      val t = authService.verifyToken(token.get).flatMap(from => {
        //get and verify correspondent
        UserDAO.findByUserName(to.get).flatMap(to => {
          //verify if not blocked
          MessageDAO.isUserBlocked(from.id.get, to.id.get).flatMap {
            case Some(x) => x.user_blocked match {
              case true => Future {
                ActorFlow.actorRef(out => ErrorMessageActor.props(out, "This user has blocked you."))
              }
              case false => notifyAndCreateActor(to, from)
            }
            case None => notifyAndCreateActor(to, from)
          }
        }
        )
      })
      Await.result(t, scala.concurrent.duration.Duration.Inf)
    }
    catch {
      case _: NoSuchElementException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Missing auth or wrong correspondent."))
      case _: NullPointerException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Outdated auth"))
      case _: JwtLengthException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Invalid auth."))
      case _: UnsupportedOperationException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Invalid auth."))
      case cause => println(cause); ActorFlow.actorRef(out => ErrorMessageActor.props(out, cause.getMessage))
    }
  }

  /**
    * Websocket endpoint to connect to the notification live system.
    * This require an authenticated user. See UserAction for more details.
    *
    * @return a string response to the user.
    */
  def notification(token: Option[String]) = WebSocket.accept[String, String] { request =>
    try {
      //verify token
      val t = authService.verifyToken(token.get).map(user =>
        ActorFlow.actorRef(out => NotificationActor.props(out, "Suscribed to notifications", user)))
      Await.result(t, scala.concurrent.duration.Duration.Inf)
    } catch {
      case _: NoSuchElementException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Missing auth"))
      case _: NullPointerException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Outdated auth"))
      case _: JwtLengthException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Invalid auth."))
      case _: UnsupportedOperationException => ActorFlow.actorRef(out => ErrorMessageActor.props(out, "Invalid auth."))
      case cause => println(cause); ActorFlow.actorRef(out => ErrorMessageActor.props(out, cause.getMessage))
    }
  }
}