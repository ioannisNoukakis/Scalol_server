package models

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

import scala.collection.mutable

/**
  * Created by durza9390 on 06.06.2017.
  */

object ChatRoom {
  var queue: mutable.Set[ChatRoom] = mutable.Set()
  def props(out: ActorRef, user: String, to: String) = Props(new ChatRoom(user, to, out))
}

case class ChatRoom (from: String, to: String, out: ActorRef) extends Actor {
  override def preStart() = {
    super.preStart()
    ChatRoom.queue += this
  }

  def receive = {
    case msg: String => {
      ChatRoom.queue.filter(_.from == to).foreach(p => {
        println("Sending: from: " + from  + " to: " + to + " message: " + msg)
        p.out ! msg
      })
    }
  }

  override def postStop() = {
    ChatRoom.queue -= this
  }

  case class Message(from: String, to: String, msg: String)

}

object ErrorMessageActor{
  def props(out: ActorRef, msg: String) = Props(new ErrorMessageActor(msg, out))
}

case class ErrorMessageActor(msg: String, out: ActorRef) extends Actor {
  override def receive: Receive = {
    case _: String => {
      out ! msg
      self ! PoisonPill
    }
  }

  def sendError() = {
    out ! msg
    self ! PoisonPill
  }
}