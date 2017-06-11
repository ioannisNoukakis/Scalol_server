package WS

import akka.actor.{Actor, ActorRef, Props}
import models.User

import scala.collection.mutable

/**
  * Created by lux on 10.06.17.
  */
object NotificationActor{
  var clients: mutable.Set[NotificationActor] = mutable.Set()
  def props(out: ActorRef, wellcomeMsg: String, user: User) = Props(new NotificationActor(user, wellcomeMsg, out))
}

case class NotificationActor(user: User, wellcomeMsg: String, out: ActorRef) extends Actor {

  override def preStart() = {
    super.preStart()
    NotificationActor.clients += this
    out ! wellcomeMsg
  }

  override def receive: Receive = {
    case m: String => out! m
  }

  def sendNotification(notification: String) = {
    out ! notification
  }

  override def postStop() = {
    NotificationActor.clients -= this
  }
}
