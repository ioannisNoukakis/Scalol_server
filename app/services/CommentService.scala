package services

import javax.inject.Inject

import models.{Comment, CommentTableDef}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by durza9390 on 25.05.2017.
  */
class CommentService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  val comments = TableQuery[CommentTableDef]

  def insert(comment: Comment)(implicit ec: ExecutionContext): Future[Comment] = {
    val insertQuery = comments returning comments.map(_.id) into ((u, id) => u.copy(id = Some(id)))
    val action = insertQuery += comment
    db.run(action)
  }

  def getByPostId(post_id: Long)(implicit ec: ExecutionContext): Future[Seq[Comment]] = {
    db.run(comments.filter(c => c.post_id === post_id).result)
  }
}
