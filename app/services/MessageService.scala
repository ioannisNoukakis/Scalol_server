package services

import javax.inject.Inject

import models.{Message, MessageTableDef}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by durza9390 on 28.05.2017.
  */
class MessageService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  val messages = TableQuery[MessageTableDef]

  def insert(message: Message)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(messages += message).map(_ => ())
  }

  def getLastMessages(user_1: Long, user_2: Long)(implicit ec: ExecutionContext): Future[Seq[Message]] = {
    db.run(messages.filter(m => m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1)
      .sortBy(_.date.desc).take(100).result)
  }

  def updateBlockFromUser(user_1: Long, user_2: Long, update: Boolean)(implicit ec: ExecutionContext): Future[Unit] = {
    val q = for {m <- messages if m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1} yield m.user_blocked
    val updateAction = q.update(update)
    db.run(updateAction).map(_ => ())
  }

  def isUserBlocked(user_1: Long, user_2: Long)(implicit ec: ExecutionContext): Future[Option[Message]] = {
    db.run(messages.filter(m => m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1).result.headOption)
  }

  def updateViewed(user_1: Long, user_2: Long)(implicit ec: ExecutionContext): Future[Unit] = {
    val q = for {m <- messages
                 if( m.first_id === user_1 && m.second_id === user_2 && m.viewed === false
                 || m.first_id === user_2 && m.second_id === user_1 &&  m.viewed === false)
    } yield m.viewed
    val updateAction = q.update(true)
    db.run(updateAction).map(_ => ())
  }
}
