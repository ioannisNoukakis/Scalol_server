package services

import javax.inject.Inject

import models.{Comment, CommentTableDef}
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * Comment service
  */
class CommentService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[Comment, CommentTableDef]{
  override protected val table: TableQuery[CommentTableDef] = TableQuery[CommentTableDef]

  /**
    * Finds a post by it's id
    * @param p the post id
    * @return
    */
  def findByPostId(p: Long): Future[Seq[Comment]] = {
    db.run(table.filter(_.post_id === p).result)
  }
}
