package WS

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

/**
  * Error message actor
  */
object ErrorMessageActor{
  def props(out: ActorRef, msg: String) = Props(new ErrorMessageActor(msg, out))
}

/**
  * Send an error messages and dies.
  *
  * @param msg the message
  * @param out the websocket of the client
  */
case class ErrorMessageActor(msg: String, out: ActorRef) extends Actor {

  override def preStart() = {
    super.preStart()
    out ! msg
  }

  override def receive: Receive = {
    case _: String => self ! PoisonPill
  }
}
