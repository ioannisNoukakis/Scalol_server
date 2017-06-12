package models

import slick.driver.MySQLDriver.api._

/**
  * Abstract model for the CommonService.
  */
abstract class BaseModel(id: Option[Long])

abstract class BaseModelTableDef[T](tag: Tag, name: String) extends Table[T](tag, name){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
}