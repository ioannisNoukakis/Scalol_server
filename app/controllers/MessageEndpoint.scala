package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Singleton

import com.google.inject.Inject
import models.{Message, MessageFrom, MessageTo}
import play.api.mvc.{BodyParsers, Controller}
import services.{MessageService, UserService}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, Json}

import scala.concurrent.Future

/**
  * Created by durza9390 on 28.05.2017.
  */
@Singleton
class MessageEndpoint @Inject()(MessageDAO: MessageService, UserDAO: UserService) extends Controller  {

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
            case true => Future {Forbidden(Json.obj("cause" -> "This user has blocked you."))}
            case false => MessageDAO.insert(Message(tmpM.content, false, false, new Date(Calendar.getInstance().getTime().getTime), request.userSession.user_id, u.id.get, None))
              .map(_ => Ok(Json.obj("state" -> "ok")))
          })
        })
          .recover { case cause => cause.printStackTrace();NotFound(Json.obj("reason" -> cause.getMessage)) }
      }
    )
  }

  def getMessages(from_username: String) = UserAction.async { implicit request =>
    UserDAO.findByUserName(from_username).flatMap(u => {
      MessageDAO.getLastMessages(request.userSession.user_id, u.id.get).map(r => Ok(Json.toJson(r.map(m => MessageFrom(u.username, m.content, m.date.toString, m.viewed, m.user_blocked)))))
    })
      .recover { case cause => NotFound(Json.obj("reason" -> cause.getMessage)) }
  }
}