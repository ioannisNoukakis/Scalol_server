package services

import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import javax.inject.Inject

import models.{BaseModel, BaseModelTableDef}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent._

/**
  * Created by lux on 06/06/2017.
  */
abstract class CommonService[T <: BaseModel, TableDef <: BaseModelTableDef[T]] extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val table : TableQuery[TableDef]

  def all(): Future[Seq[T]] = db.run(table.take(100).result)

  def insert(item: T)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(table += item).map(_ => ())
  }

  def findById(id: Long)(implicit ec: ExecutionContext): Future[T] = {
    db.run(table.filter(_.id === id).result).map(dbObject => dbObject.head)
  }

  def update(id: Long, model: T)(implicit ec: ExecutionContext): Future[Unit] = {
    val q = for {u <- table if u.id === id} yield u
    val updateAction = q.update(model)
    db.run(updateAction).map(_ => ())
  }

  def delete(user_id: Long)(implicit ec: ExecutionContext): Future[Unit] = {
    db.run(table.filter(_.id === user_id).delete).map(_=>())
  }
}
