package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.Iterable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object BidActor {
  case class Bid(bidId: Int, auctionId: Int, offer: Float)

  sealed trait Command

  // Request a list of bids for an auction
  final case class RequestAuctionBids(auctionId: Int, replyTo: ActorRef[Response]) extends Command

  // Interface of actor outgoing messages (Responses)
  trait Response

  final case class BidList(auctions: Iterable[Bid]) extends Response
  final case class BidListFetchingError(error: String) extends Response

  /**
    * Actor builder method
    */
  def apply(bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("AuctionActor started")

      Behaviors.receiveMessage {
        case RequestAuctionBids(auctionId, replyTo) =>
          val bidsFuture: Future[Iterable[Bid]] = bidDao.ofAuction(auctionId)
          bidsFuture.onComplete {
            case Success(bidsIterable) => replyTo ! BidList(bidsIterable)
            case Failure(e)            => replyTo ! BidListFetchingError(e.getMessage)
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
}
