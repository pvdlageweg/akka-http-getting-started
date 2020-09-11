package nl.pvdlageweg.akkahttp

import nl.pvdlageweg.akkahttp.BidActor.Bid
import nl.pvdlageweg.akkahttp.BidDaoDefinitions.BidDaoImpl
import slick.lifted.ProvenShape

import scala.concurrent.Future

private[akkahttp] trait BidDao {

  def create(bidInformation: Bid): Future[Unit]

  def read(auctionId: Int): Future[Option[Bid]]

  def all(): Future[Iterable[Bid]]

  def ofAuction(auctionId: Int): Future[Iterable[Bid]]
}

object BidDao {
  def apply(): BidDao = new BidDaoImpl()
}

private[akkahttp] object BidDaoDefinitions extends SlickDAO[Bid, Bid] {

  import dbProfile.profile.api._

  override lazy val query: TableQuery[BidTable] = TableQuery[BidTable]

  override def toRow(domainObject: Bid): Bid = ???

  override def fromRow(dbRow: Bid): Bid = ???

  class BidTable(tag: Tag) extends Table[Bid](tag, "bids") {
    override def * : ProvenShape[Bid] = (bidId, auctionId, offer).<>((Bid.apply _).tupled, Bid.unapply)

    def bidId = column[Int]("bid_id", O.PrimaryKey)

    def auctionId = column[Int]("auction_id")

    def offer = column[Float]("offer")
  }

  class BidDaoImpl extends BidDao with ActorContext {
    override def create(bidInformation: Bid): Future[Unit] = {
      db.run(query += bidInformation).map(_ => ())
    }

    override def read(bidId: Int): Future[Option[Bid]] = {
      db.run(query.filter(e => e.bidId === bidId).result)
        .map(_.headOption)
    }

    override def all(): Future[Iterable[Bid]] =
      db.run(query.result)
        .map(xs => xs.map(row => Bid(row.bidId, row.auctionId, row.offer)))

    override def ofAuction(auctionId: Int): Future[Iterable[Bid]] =
      db.run(query.filter(e => e.auctionId === auctionId).result)
        .map(xs => xs.map(row => Bid(row.bidId, row.auctionId, row.offer)))
  }
}
