package services

import models.{Post, PostTableDef, UserUpvotes, UserUpvotesTableDef}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent._

/**
  * Created by lux on 19/05/2017.
  */
class PostService @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[Post, PostTableDef] {
  override protected val table: TableQuery[PostTableDef] = TableQuery[PostTableDef]
  val usersUpvotes = TableQuery[UserUpvotesTableDef]

  def addPost(post: Post)(implicit ec: ExecutionContext): Future[Post] = {
    val insertQuery = table returning table.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += post
    db.run(action)
  }

  def all(offset: Long, number: Long): Future[Seq[Post]] = offset match {
    case -1 => db.run(table.sortBy(_.id.desc).take(number).result)
    case _ => db.run(table.sortBy(_.id.desc).filter(p => p.id <= offset).take(number).result)
  }

  def updateUserAndPostUpvotesOrFalse(post_id: Long, user_id: Long, inc: Int)(implicit ec: ExecutionContext): Future[Boolean] = {
    db.run(usersUpvotes.filter(p => p.post_id === post_id && p.user_id === user_id).result.headOption).map {
      case Some(x) => {
        if (inc == 1 && !x.inc || inc == -1 && x.inc) {
          val q = for {p <- usersUpvotes if p.post_id === post_id && p.user_id === user_id} yield p.inc
          db.run(q.update(inc == 1).map(_ => ()))
          true
        }
        else
          false
      }
      case None => {
        db.run(usersUpvotes += UserUpvotes(inc == 1, post_id, user_id)).map(_ => ())
        true
      }
    }
  }

  def modifyScore(post_id: Long, inc: Int)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(table.filter(p => p.id === post_id).result).map(dbObject => {
      val q = for {p <- table if p.id === post_id} yield p.score
      val updateAction = q.update(dbObject.head.score + inc)
      db.run(updateAction)
    })
  }

  def getUserPosts(user_id: Long)(implicit ec: ExecutionContext): Future[Seq[Post]] = {
    db.run(table.filter(p => p.owner_id === user_id).result)
  }
}
