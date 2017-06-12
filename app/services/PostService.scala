package services

import models.{Post, PostTableDef, UserUpvotes, UserUpvotesTableDef}
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent._

/**
  * Post service
  */
class PostService @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[Post, PostTableDef] {
  override protected val table: TableQuery[PostTableDef] = TableQuery[PostTableDef]
  val usersUpvotes = TableQuery[UserUpvotesTableDef]

  /**
    * Adds a post.
    * @param post the post
    * @return a post with it's DB id
    */
  def addPost(post: Post): Future[Post] = {
    val insertQuery = table returning table.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += post
    db.run(action)
  }

  /**
    * Get some posts
    * @param offset the offset where to start taking post
    * @param number the number of posts to take
    * @return seq post
    */
  def all(offset: Long, number: Long): Future[Seq[Post]] = offset match {
    case -1 => db.run(table.sortBy(_.id.desc).take(number).result)
    case _ => db.run(table.sortBy(_.id.desc).filter(p => p.id <= offset).take(number).result)
  }

  /**
    * check and set the upvote/downvote flag for a user.
    * In the DB there is a table that symbolize the relation between user and posts.
    * This service is used to set if the user can upvote and if so it make it impossible
    * to upvote again.
    *
    * @param post_id the post
    * @param user_id the user
    * @param inc the increment 1 or -1
    * @return true true if the user had downvoted or upvoted the post before
    *         true false if the user had not upvoted or downvoted the post before
    *         false false if the user had already upvoted / downvoted
    */
  def updateUserAndPostUpvotesOrFalse(post_id: Long, user_id: Long, inc: Int): Future[(Boolean, Boolean)] = {
    db.run(usersUpvotes.filter(p => (p.post_id === post_id) && (p.user_id === user_id)).result.headOption).flatMap {
      case Some(x) => {
        if (inc == 1 && !x.inc || inc == -1 && x.inc) {
          val q = for {p <- usersUpvotes if p.post_id === post_id && p.user_id === user_id} yield p.inc
          db.run(q.update(inc == 1)).map(_ => (true, true))
        }
        else
          Future {
            (false, false)
          }
      }
      case None => {
        db.run(usersUpvotes += UserUpvotes(inc == 1, post_id, user_id)).map(_ => (true, false))
      }
    }
  }

  /**
    * Modify the score of a post.
    *
    * @param post_id the post
    * @param inc the inc
    * @return futur unit
    */
  def modifyScore(post_id: Long, inc: Int): Future[Unit] = {
    db.run(table.filter(p => p.id === post_id).result).map(dbObject => {
      val q = for {p <- table if p.id === post_id} yield p.score
      val updateAction = q.update(dbObject.head.score + inc)
      db.run(updateAction)
    })
  }

  /**
    * Get the posts made by this user.
    *
    * @param user_id the user
    * @return seq post
    */
  def getUserPosts(user_id: Long): Future[Seq[Post]] = {
    db.run(table.filter(p => p.owner_id === user_id).result)
  }
}
