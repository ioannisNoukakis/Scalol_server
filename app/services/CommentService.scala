package services

import javax.inject.Inject

import models.{Comment, CommentTableDef}
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.TableQuery

/**
  * Created by durza9390 on 25.05.2017.
  */
class CommentService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends CommonService[Comment, CommentTableDef]{
  override protected val table: TableQuery[CommentTableDef] = TableQuery[CommentTableDef]
}
