package services

import javax.inject.Inject

import models.{Message, MessageTableDef}
import play.api.db.slick.{DatabaseConfigProvider}

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.MySQLDriver.api._

import scala.concurrent.{Future}

/**
  * Message service
  */
class MessageService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[Message, MessageTableDef] {
  import Message.getMessageResult

  override protected val table: TableQuery[MessageTableDef] = TableQuery[MessageTableDef]

  /**
    * Get the last 100 messages between two users.
    *
    * @param user_1 first user
    * @param user_2 second user
    * @return the messages
    */
  def getLastMessages(user_1: Long, user_2: Long): Future[Seq[Message]] = {
    db.run(table.filter(m => m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1)
      .sortBy(_.date.desc).take(100).result)
  }

  /**
    * Update the block state from a user.
    *
    * @param user_1 the first user
    * @param user_2 the second user
    * @param update true if you wanna block or false if no block
    * @return a futur of unit
    */
  def updateBlockFromUser(user_1: Long, user_2: Long, update: Boolean): Future[Unit] = {
    val q = for {m <- table if m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1} yield m.user_blocked
    val updateAction = q.update(update)
    db.run(updateAction).map(_ => ())
  }

  /**
    * Checks if a user is blocked
    *
    * @param user_1 user 1
    * @param user_2 user 2
    * @return the message between two users
    */
  def isUserBlocked(user_1: Long, user_2: Long): Future[Option[Message]] = {
    db.run(table.filter(m => m.first_id === user_1 && m.second_id === user_2 || m.first_id === user_2 && m.second_id === user_1).result.headOption)
  }

  /**
    * Updates if the user has viwed the messages of a conversation
    * @param user_1 the first user
    * @param user_2 the second user
    * @return a future of unit
    */
  def updateViewed(user_1: Long, user_2: Long): Future[Unit] = {
    val q = for {m <- table
                 if( m.first_id === user_1 && m.second_id === user_2 && m.viewed === false
                   || m.first_id === user_2 && m.second_id === user_1 &&  m.viewed === false)
    } yield m.viewed
    val updateAction = q.update(true)
    db.run(updateAction).map(_ => ())
  }

  /**
    * Gets all the conversations for a user.
    * @param user_id the user
    * @return seq message
    */
  def getUserMailBox(user_id: Long): Future[Seq[Message]] = {
    db.run(
      sql"""SELECT * FROM message WHERE first_id = $user_id
           or second_id = $user_id GROUP BY first_id, second_id""".as[Message])
  }
}
