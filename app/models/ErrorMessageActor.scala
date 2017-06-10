package models

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

/**
  * Created by lux on 10.06.17.
  */
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
