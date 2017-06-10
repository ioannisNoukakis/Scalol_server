package services

import javax.inject.Inject

import models.{Message, MessageTableDef}
import play.api.db.slick.{DatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.MySQLDriver.api._

import scala.concurrent.{Future}

/**
  * Created by durza9390 on 28.05.2017.
  */
class MessageService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[Message, MessageTableDef] {
  import Message.getMessageResult

  override protected val table: TableQuery[MessageTableDef] = TableQuery[MessageTableDef]

  def getLastMessages(user_1: Long, user_2: Long): Future[Seq[Message]] = {
    db.run(table.filter(m => m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1)
      .sortBy(_.date.desc).take(100).result)
  }

  def updateBlockFromUser(user_1: Long, user_2: Long, update: Boolean): Future[Unit] = {
    val q = for {m <- table if m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1} yield m.user_blocked
    val updateAction = q.update(update)
    db.run(updateAction).map(_ => ())
  }

  def isUserBlocked(user_1: Long, user_2: Long): Future[Option[Message]] = {
    db.run(table.filter(m => m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1).result.headOption)
  }

  def updateViewed(user_1: Long, user_2: Long): Future[Unit] = {
    val q = for {m <- table
                 if( m.first_id === user_1 && m.second_id === user_2 && m.viewed === false
                   || m.first_id === user_2 && m.second_id === user_1 &&  m.viewed === false)
    } yield m.viewed
    val updateAction = q.update(true)
    db.run(updateAction).map(_ => ())
  }

  def getUserMailBox(user_id: Long): Future[Seq[Message]] = {
    db.run(sql"""SELECT * FROM message WHERE first_id = $user_id or second_id = $user_id GROUP BY first_id, second_id""".as[Message])
  }
}
