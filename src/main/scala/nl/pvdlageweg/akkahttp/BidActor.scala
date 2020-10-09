package nl.pvdlageweg.akkahttp

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior

import scala.collection.Iterable
import scala.concurrent.ExecutionContext

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

  sealed trait Event
  final case class State()

  def apply(auctionDoa: AuctionDao, bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("abc"),
      emptyState = State(),
      commandHandler = (state, cmd) => throw new NotImplementedError("TODO: process the command & return an Effect"),
      eventHandler = (state, evt) => throw new NotImplementedError("TODO: process the event return the next state")
    )

//  def apply(auctionDoa: AuctionDao, bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] =
//    Behaviors.setup { context =>
//      context.log.info("AuctionActor started")
//
//      Behaviors.receiveMessage {
//        case RequestAuctionBids(auctionId, replyTo) =>
//          val bidsFuture: Future[Iterable[Bid]] = bidDao.ofAuction(auctionId)
//          bidsFuture.onComplete {
//            case Success(bidsIterable) => replyTo ! BidList(bidsIterable)
//            case Failure(e)            => replyTo ! BidListFetchingError(e.getMessage)
//          }
//          Behaviors.same
//
//        case RequestPlaceAuctionBid(bidRequest, replyTo) =>
//          val auctionFuture: Future[Option[Auction]] = auctionDoa.read(bidRequest.auctionId)
//          auctionFuture.onComplete {
//            case Success(optionAuction) =>
//              optionAuction match {
//                case Some(_) =>
//                  println("Found auction")
//                  val bidsFuture = bidDao.ofAuction(bidRequest.auctionId)
//                  bidsFuture.onComplete {
//                    case Success(bidsIterator) =>
//                      val maxBid = bidsIterator.map(_.offer).max
//                      println(s"max bix $maxBid")
//                      if (bidRequest.offer <= maxBid) {
//
//                        replyTo ! BidPlacementFailed(s"Bid is less then current top bid of $maxBid")
//                      } else {
//                        val saveBidFuture = bidDao.create(bidRequest)
//                        saveBidFuture.onComplete {
//                          case Success(_) => replyTo ! BidPlacementSuccessful()
//                          case Failure(e) => replyTo ! BidPlacementFailed(e.getMessage)
//                        }
//                      }
//
//                    case Failure(e) => replyTo ! BidPlacementFailed(e.getMessage)
//                  }
//                case None =>
//                  replyTo ! BidPlacementFailed("Auction not found")
//              }
//            case Failure(e) => replyTo ! BidPlacementFailed(e.getMessage)
//          }
//
//          Behaviors.same
//        case _ =>
//          Behaviors.unhandled
//      }
//    }
}
