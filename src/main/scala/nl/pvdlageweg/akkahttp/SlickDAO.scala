package nl.pvdlageweg.akkahttp

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait SlickDAO[DbRow, DomainObject] {
  protected val dbProfile: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("db.auctions")
  protected val db = dbProfile.db
  import dbProfile.profile.api._

  val query: TableQuery[_ <: Table[DbRow]]

  def toRow(domainObject: DomainObject): DbRow

  def fromRow(dbRow: DbRow): DomainObject

}
