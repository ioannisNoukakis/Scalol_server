package WS

import java.sql.Date
import java.util.Calendar

import akka.actor.{Actor, ActorRef, Props}
import models._
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import slick.lifted.TableQuery

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Chat actor
  */
object ChatActor {
  val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val messages = TableQuery[MessageTableDef]

  var clients: mutable.Set[ChatActor] = mutable.Set()
  def props(out: ActorRef, from: User, to: User) = Props(new ChatActor(from, to, out))
}

/**
  * Chat actor from the live chat system. It's like a stream of letters from a user to an other.
  *
  * @param from the user that sens messages
  * @param to the user that receive messages
  * @param out the connection with the websocket of this client so it can receive the
  *            messages from the other user (to).
  */
case class ChatActor(from: User, to: User, out: ActorRef) extends Actor {

  /**
    * Here we register the actor to the live system and we say hello.
    */
  override def preStart() = {
    super.preStart()
    ChatActor.clients += this
    val msg = "Wellcome to FouZiTout messenger 1.0\n"
    ChatActor.clients.exists(_.from == to) match {
      case true => out ! msg + "Your correspondent, " + to.username + " is online!"
      case false => out ! msg + "Your correspondent, " + to.username + " is offline but you can send a message!" +
        " He/her/it/apache helicopter will receive it at their next connection..."
    }
  }

  /**
    * When we receive a message from the client we forward it to the destination.
    * @return
    */
  def receive = {
    case msg: String => {
      ChatActor.clients.filter(_.from == to).foreach(p => {
        ChatActor.db.run(ChatActor.messages += Message(msg, true, false,
          new Date(Calendar.getInstance().getTime.getTime), from.id.get, to.id.get, None)).map(_ => ())
        NotificationActor.clients.filter(_.user == to).foreach(_.sendNotification(from.username +
          " has sent you a message!"))
        println("Sending: from: " + from.username  + " to: " + to.username + " message: " + msg)
        p.out ! "[" + from.username + "] " + msg
      })
    }
  }

  /**
    * Upon disconnection, we remove the client from the live system.
    */
  override def postStop() = {
    ChatActor.clients.filter(_.from == to).foreach(_.out ! from.username + " has disconnected." )
    ChatActor.clients -= this
  }
}