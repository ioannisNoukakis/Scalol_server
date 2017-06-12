package WS

import akka.actor.{Actor, ActorRef, Props}
import models.User

import scala.collection.mutable

/**
  * Notification actor
  */
object NotificationActor{
  var clients: mutable.Set[NotificationActor] = mutable.Set()
  def props(out: ActorRef, wellcomeMsg: String, user: User) = Props(new NotificationActor(user, wellcomeMsg, out))
}

/**
  * Notifies a user on server events.
  *
  * @param user the user to notify
  * @param wellcomeMsg the welcome message
  * @param out the websocket of the client
  */
case class NotificationActor(user: User, wellcomeMsg: String, out: ActorRef) extends Actor {

  /**
    * On startup we register this actor the the live notification system.
    */
  override def preStart() = {
    super.preStart()
    NotificationActor.clients += this
    out ! wellcomeMsg
  }

  /**
    * Upon receive we echo
    * @return
    */
  override def receive: Receive = {
    case m: String => out! m
  }

  /**
    * Sens a notification
    * @param notification the message
    */
  def sendNotification(notification: String) = {
    out ! notification
  }

  /**
    * Upon leaving we remove the actor from the live notification system.
    */
  override def postStop() = {
    NotificationActor.clients -= this
  }
}
