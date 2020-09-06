package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.{ExecutionContext, Future}

object AuctionActor {
  case class Auction(auctionId: Int, description: String)
  case class Bid(auctionId: Int, userId: String, offer: Int)
  sealed trait AuctionCommands
  case class PlaceBid(bid: Bid, replyTo: ActorRef[BidsAcceptance]) extends AuctionCommands
  case class GetBids(replyTo: ActorRef[AuctionCommands]) extends AuctionCommands
  case class Bids(bids: List[Bid]) extends AuctionCommands

  sealed trait BidsAcceptance extends AuctionCommands
  case class BidRejected(error: String) extends BidsAcceptance
  case class BidAccepted() extends BidsAcceptance

  case class RequestAuctionList(replyTo: ActorRef[AuctionCommands]) extends AuctionCommands
  case class AuctionList(auctions: Iterable[Auction]) extends AuctionCommands

  /**
    * Actor builder method
    */
  def apply(auctionDoa: AuctionDao)(implicit executionContext: ExecutionContext): Behavior[AuctionCommands] =
    apply(auctionDoa, List.newBuilder.+=(Auction(223, "test auction")).result(), List.empty[Bid])

  def apply(auctionDao: AuctionDao, auctions: List[Auction], bids: List[Bid])(implicit
      executionContext: ExecutionContext
  ): Behavior[AuctionCommands] =
    Behaviors.setup { context =>
      context.log.info("AuctionActor started")

      Behaviors.receiveMessage {
        case RequestAuctionList(replyTo) =>
          val auctions: Future[Iterable[Auction]] = auctionDao.all()
          auctions.onComplete {
            case scala.util.Success(result) =>
              replyTo ! AuctionList(result)
            case scala.util.Failure(ex) =>
              replyTo ! BidRejected(ex.getMessage)
          }
          Behaviors.same
        case PlaceBid(bid: Bid, replyTo: ActorRef[BidsAcceptance]) =>
          context.log.info(s"Bid recieved: $bid.userId, $bid.offer")

          auctions.find(Auction => Auction.auctionId == bid.auctionId) match {
            case Some(_) =>
              val higherBidFound = bids.find(Bid => Bid.offer > bid.offer)
              higherBidFound match {
                case Some(bid) =>
                  val error = s"Found higher bid: ${bid.offer}"
                  println(error)
                  replyTo ! BidRejected(error)
                  Behaviors.same
                case None =>
                  println("No higher bid found")
                  replyTo ! BidAccepted()
                  apply(auctionDao, auctions, bids :+ bid)
              }
            case None =>
              val error = s"No auction found with id ${bid.auctionId}"
              println(error)
              replyTo ! BidRejected(error)
              Behaviors.same
          }
        case GetBids(replyTo) =>
          replyTo ! Bids(bids)
          Behaviors.same
        case _ =>
          Behaviors.unhandled
      }
    }
}
