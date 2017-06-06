package models

import slick.driver.MySQLDriver.api._

/**
  * Created by lux on 06.06.17.
  */
abstract class BaseModel(id: Option[Long])

abstract class BaseModelTableDef[T](tag: Tag, name: String) extends Table[T](tag, name){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
}