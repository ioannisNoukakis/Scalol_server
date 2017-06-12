package services

import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

import models.{BaseModel, BaseModelTableDef}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent._

/**
  * Common abstract service or all basic operations such as get all, insert, find by id, update, delete
  */
abstract class CommonService[T <: BaseModel, TableDef <: BaseModelTableDef[T]] extends HasDatabaseConfigProvider[JdbcProfile] {

  protected val table : TableQuery[TableDef]

  def all(): Future[Seq[T]] = db.run(table.take(100).result)

  def insert(item: T): Future[Unit] = {
    db.run(table += item).map(_ => ())
  }

  def findById(id: Long): Future[T] = {
    db.run(table.filter(_.id === id).result).map(dbObject => dbObject.head)
  }

  def update(id: Long, model: T): Future[Unit] = {
    val q = for {u <- table if u.id === id} yield u
    val updateAction = q.update(model)
    db.run(updateAction).map(_ => ())
  }

  def delete(user_id: Long): Future[Unit] = {
    db.run(table.filter(_.id === user_id).delete).map(_=>())
  }
}
