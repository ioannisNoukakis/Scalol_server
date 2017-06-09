package models

import java.sql.Date
import java.util.Calendar

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.lifted.TableQuery

import scala.collection.mutable

/**
  * Created by durza9390 on 06.06.2017.
  */

object ChatRoom {
  val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val messages = TableQuery[MessageTableDef]

  var clients: mutable.Set[ChatRoom] = mutable.Set()
  def props(out: ActorRef, from: User, to: User) = Props(new ChatRoom(from, to, out))
}

case class ChatRoom (from: User, to: User, out: ActorRef) extends Actor {

  override def preStart() = {
    super.preStart()
    ChatRoom.clients += this
    val msg = "Wellcome to FouZiTout messenger 1.0\n"
    ChatRoom.clients.exists(_.from == to) match {
      case true => out ! msg + "Your correspondent, " + to.username + " is online!"
      case false => out ! msg + "Your correspondent, " + to.username + " is offline but you can send a message!" +
        " He/her/it/apache helicopter will receive it at their next connection..."
    }
  }

  def receive = {
    case msg: String => {
      ChatRoom.clients.filter(_.from == to).foreach(p => {
        val future = ChatRoom.db.run(ChatRoom.messages
          += Message(msg, true, false, new Date(Calendar.getInstance().getTime().getTime), from.id.get, to.id.get, None)).map(_ => ())
        println("Sending: from: " + from.username  + " to: " + to.username + " message: " + msg)
        p.out ! "[" + from.username + "]" + msg
      })
    }
  }
  /*
  case class Message(content: String,
                   viewed: Boolean,
                   user_blocked: Boolean,
                   date: Date,
                   first_id: Long,
                   second_id: Long,
                   id: Option[Long])
            extends BaseModel(id)
   */

  override def postStop() = {
    ChatRoom.clients -= this
  }
}

object ErrorMessageActor{
  def props(out: ActorRef, msg: String) = Props(new ErrorMessageActor(msg, out))
}

case class ErrorMessageActor(msg: String, out: ActorRef) extends Actor {

  override def preStart() = {
    super.preStart()
    out ! msg
  }

  override def receive: Receive = {
    case _: String => self ! PoisonPill
  }
}