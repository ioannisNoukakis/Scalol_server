package services

import models.{Post, PostTableDef, User}
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
class PostService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  val posts = TableQuery[PostTableDef]

  def insert(post: Post)(implicit ec: ExecutionContext): Future[Post] = {
    val insertQuery = posts returning posts.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += post
    db.run(action)
  }

  def all(): Future[Seq[Post]] = db.run(posts.sortBy(_.id.desc).take(100).result)

  def modifyScore(post_id: Long, inc: Int)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(posts.filter(p => p.id === post_id).result).map(dbObject => {
      val q = for {p <- posts if p.id === post_id} yield p.score
      val updateAction = q.update(dbObject.head.score + inc)
      db.run(updateAction)
    })
  }

  def getUserPosts(user_id: Long)(implicit ec: ExecutionContext):Future[Seq[Post]] = {
    db.run(posts.filter(p => p.owner_id === user_id).result)
  }

  def findById(post_id: Long)(implicit ec: ExecutionContext):Future[Post] = {
    db.run(posts.filter(p => p.id === post_id).result).map(dbObject => dbObject.head)
  }
}
