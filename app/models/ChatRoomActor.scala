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
import scala.reflect.internal.util.TableDef

/**
  * Created by durza9390 on 06.06.2017.
  */

object ChatRoomActor {
  val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val messages = TableQuery[MessageTableDef]

  var clients: mutable.Set[ChatRoomActor] = mutable.Set()
  def props(out: ActorRef, from: User, to: User) = Props(new ChatRoomActor(from, to, out))
}

case class ChatRoomActor(from: User, to: User, out: ActorRef) extends Actor {

  override def preStart() = {
    super.preStart()
    ChatRoomActor.clients += this
    val msg = "Wellcome to FouZiTout messenger 1.0\n"
    ChatRoomActor.clients.exists(_.from == to) match {
      case true => out ! msg + "Your correspondent, " + to.username + " is online!"
      case false => out ! msg + "Your correspondent, " + to.username + " is offline but you can send a message!" +
        " He/her/it/apache helicopter will receive it at their next connection..."
    }
  }

  def receive = {
    case msg: String => {
      ChatRoomActor.clients.filter(_.from == to).foreach(p => {
        val future = ChatRoomActor.db.run(ChatRoomActor.messages
          += Message(msg, true, false, new Date(Calendar.getInstance().getTime().getTime), from.id.get, to.id.get, None)).map(_ => ())
        println("Sending: from: " + from.username  + " to: " + to.username + " message: " + msg)
        p.out ! "[" + from.username + "]" + msg
      })
    }
  }

  override def postStop() = {
    ChatRoomActor.clients -= this
  }
}