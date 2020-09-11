package nl.pvdlageweg.akkahttp

import nl.pvdlageweg.akkahttp.AuctionActor.Auction
import nl.pvdlageweg.akkahttp.AuctionDaoDefinitions.AuctionDaoImpl
import slick.lifted.ProvenShape

import scala.concurrent.Future

private[akkahttp] trait AuctionDao {

  def create(auctionInformation: Auction): Future[Unit]

  def read(auctionId: Int): Future[Option[Auction]]

  def all(): Future[Iterable[Auction]]
}

object AuctionDao {
  def apply(): AuctionDao = new AuctionDaoImpl()
}

private[akkahttp] object AuctionDaoDefinitions extends SlickDAO[Auction, Auction] {

  import dbProfile.profile.api._

  override lazy val query: TableQuery[AuctionsTable] = TableQuery[AuctionsTable]

  override def toRow(domainObject: Auction): Auction = ???

  override def fromRow(dbRow: Auction): Auction = ???

  class AuctionsTable(tag: Tag) extends Table[Auction](tag, "auctions") {
    override def * : ProvenShape[Auction] = (auctionId, description).<>((Auction.apply _).tupled, Auction.unapply)

    def auctionId = column[Int]("auction_id", O.PrimaryKey)

    def description = column[String]("description")
  }

  class AuctionDaoImpl extends AuctionDao with ActorContext {
    override def create(auctionInformation: Auction): Future[Unit] = {
      db.run(query += auctionInformation).map(_ => ())
    }

    override def read(auctionId: Int): Future[Option[Auction]] = {
      db.run(query.filter(e => e.auctionId === auctionId).result)
        .map(_.headOption)
    }

    override def all(): Future[Iterable[Auction]] =
      db.run(query.result)
        .map(xs => xs.map(row => Auction(row.auctionId, row.description)))
  }
}
