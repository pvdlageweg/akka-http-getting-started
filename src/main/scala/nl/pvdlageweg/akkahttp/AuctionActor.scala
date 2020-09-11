package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.Iterable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
object AuctionActor {
  case class Auction(auctionId: Int, description: String)
  case class Bid(auctionId: Int, userId: String, offer: Int)

  sealed trait Command

  // Request a list of auctions
  final case class RequestAuctionList(replyTo: ActorRef[Response]) extends Command

  // Request one auction
  final case class RequestAuction(auctionId: Int, replyTo: ActorRef[Response]) extends Command

  // Interface of actor outgoing messages (Responses)
  trait Response

  final case class AuctionList(auctions: Iterable[Auction]) extends Response
  final case class AuctionListFetchingError(error: String) extends Response

  final case class ExistingAuction(auction: Auction) extends Response
  final case class AuctionNotFound(auctionId: Int) extends Response
  final case class AuctionFetchingError(error: String) extends Response

  /**
    * Actor builder method
    */
  def apply(auctionDoa: AuctionDao)(implicit executionContext: ExecutionContext): Behavior[Command] =
    apply(auctionDoa, List.empty[Bid])

  def apply(auctionDao: AuctionDao, bids: List[Bid])(implicit
      executionContext: ExecutionContext
  ): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("AuctionActor started")

      Behaviors.receiveMessage {
        case RequestAuctionList(replyTo) =>
          val auctionsFuture: Future[Iterable[Auction]] = auctionDao.all()
          auctionsFuture.onComplete {
            case Success(auctionsIterable) => replyTo ! AuctionList(auctionsIterable)
            case Failure(e)                => replyTo ! AuctionListFetchingError(e.getMessage)
          }
          Behaviors.same

        case RequestAuction(auctionId, replyTo) =>
          val auctionFuture: Future[Option[Auction]] = auctionDao.read(auctionId)
          auctionFuture.onComplete {
            case Success(auction) =>
              replyTo ! auction.map(x => ExistingAuction(x)).getOrElse(AuctionNotFound(auctionId))
            case Failure(e) => replyTo ! AuctionFetchingError(e.getMessage)
          }
          Behaviors.same

//        case PlaceBid(bid: Bid, replyTo: ActorRef[BidsResponse]) =>
//          context.log.info(s"Bid recieved: $bid.userId, $bid.offer")
//
//          auctions.find(Auction => Auction.auctionId == bid.auctionId) match {
//            case Some(_) =>
//              val higherBidFound = bids.find(Bid => Bid.offer > bid.offer)
//              higherBidFound match {
//                case Some(bid) =>
//                  val error = s"Found higher bid: ${bid.offer}"
//                  println(error)
//                  replyTo ! BidRejected(error)
//                  Behaviors.same
//                case None =>
//                  println("No higher bid found")
//                  replyTo ! BidAccepted()
//                  apply(auctionDao, auctions, bids :+ bid)
//              }
//            case None =>
//              val error = s"No auction found with id ${bid.auctionId}"
//              println(error)
//              replyTo ! BidRejected(error)
//              Behaviors.same
//          }
//        case GetBids(replyTo) =>
//          replyTo ! Bids(bids)
//          Behaviors.same
        case _ =>
          Behaviors.unhandled
      }
    }
}
