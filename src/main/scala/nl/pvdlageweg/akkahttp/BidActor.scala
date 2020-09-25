package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import nl.pvdlageweg.akkahttp.AuctionActor.Auction

import scala.collection.Iterable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object BidActor {
  case class Bid(bidId: Int, auctionId: Int, offer: Float)
  case class BidRequest(auctionId: Int, offer: Float)

  sealed trait Command

  // Request a list of bids for an auction
  final case class RequestAuctionBids(auctionId: Int, replyTo: ActorRef[Response]) extends Command

  // Put a bid on an auction
  final case class RequestPlaceAuctionBid(bid: BidRequest, replyTo: ActorRef[Response]) extends Command

  // Interface of actor outgoing messages (Responses)
  trait Response

  final case class BidList(auctions: Iterable[Bid]) extends Response
  final case class BidListFetchingError(error: String) extends Response

  final case class BidPlacementSuccessful() extends Response
  final case class BidPlacementFailed(error: String) extends Response

  /**
    * Actor builder method
    */
  def apply(auctionDoa: AuctionDao, bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] =
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

        case RequestPlaceAuctionBid(bidrequest, replyTo) =>
          val auctionFuture: Future[Option[Auction]] = auctionDoa.read(bidrequest.auctionId)
          auctionFuture.onComplete {
            case Success(optionAuction) => {
              optionAuction match {
                case Some(optionAuction) =>
                  println("Found auction")
                  val bidsFuture = bidDao.ofAuction(bidrequest.auctionId)
                  bidsFuture.onComplete {
                    case Success(bidsIterator) => {
                      val maxBid = bidsIterator.map(_.offer).max
                      println(s"max bix $maxBid")
                      if (bidrequest.offer <= maxBid) {

                        replyTo ! BidPlacementFailed(s"Bid is less then current top bid of $maxBid")
                      } else {
                        val saveBidFuture = bidDao.create(bidrequest)
                        saveBidFuture.onComplete {
                          case Success(bidsIterator) => replyTo ! BidPlacementSuccessful()
                          case Failure(e)            => replyTo ! BidPlacementFailed(e.getMessage)
                        }
                      }
                    }

                    case Failure(e) => replyTo ! BidPlacementFailed(e.getMessage)
                  }
                case None =>
                  replyTo ! BidPlacementFailed("Auction not found")
              }
            }
            case Failure(e) => replyTo ! BidPlacementFailed(e.getMessage)
          }

          Behaviors.same
        case _ =>
          Behaviors.unhandled
      }
    }
}
