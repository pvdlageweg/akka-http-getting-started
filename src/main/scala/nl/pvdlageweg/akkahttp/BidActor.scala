package nl.pvdlageweg.akkahttp

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import nl.pvdlageweg.akkahttp.AuctionActor.Auction

import scala.collection.Iterable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object BidActor {
  case class Bid(bidId: Int, auctionId: Int, offer: Float)
  case class BidRequest(auctionId: Int, offer: Float)

  sealed trait Command extends CborSerialized

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

  def apply(auctionDoa: AuctionDao, bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("abc"),
      emptyState = State(),
      commandHandler = (state, command) => onCommand(state, command, auctionDoa, bidDao),
      eventHandler = (state, event) => eventHandler(state, event)
    )
  }

  private def onCommand(state: State, command: Command, auctionDoa: AuctionDao, bidDao: BidDao)(implicit
      executionContext: ExecutionContext
  ): Effect[Event, State] = {
    command match {
      case RequestAuctionBids(auctionId, replyTo) =>
        val bidsFuture: Future[Iterable[Bid]] = bidDao.ofAuction(auctionId)
        bidsFuture.onComplete {
          case Success(bidsIterable) => replyTo ! BidList(bidsIterable)
          case Failure(e)            => replyTo ! BidListFetchingError(e.getMessage)
        }
        Effect.none
      case RequestPlaceAuctionBid(bidRequest, replyTo) =>
        val auctionFuture: Future[Option[Auction]] = auctionDoa.read(bidRequest.auctionId)
        auctionFuture.onComplete {
          case Success(optionAuction) =>
            optionAuction match {
              case Some(_) =>
                println("Found auction")
                val bidsFuture = bidDao.ofAuction(bidRequest.auctionId)
                bidsFuture.onComplete {
                  case Success(bidsIterator) =>
                    val maxBid = bidsIterator.map(_.offer).max
                    println(s"max bix $maxBid")
                    if (bidRequest.offer <= maxBid) {
                      val errorMessage = s"Bid is less then current top bid of $maxBid"
                      Effect.reply(replyTo)(BidPlacementFailed(errorMessage))
                    } else {
                      val saveBidFuture = bidDao.create(bidRequest)
                      saveBidFuture.onComplete {
                        case Success(savedBid) =>
                          println(s"Success!")
                          println(savedBid)
                          Effect.persist(bidRequest).thenReply(replyTo){x:Bid=>BidPlacementSuccessful()}
                        case Failure(e) => Effect.reply(replyTo){BidPlacementFailed(e.getMessage)}
                      }
                    }

                  case Failure(e) => Effect.reply(replyTo)(BidPlacementFailed(e.getMessage))
                }
              case None =>
                Effect.reply(replyTo)(BidPlacementFailed("Auction not found"))
            }
          case Failure(e) => Effect.reply(replyTo)(BidPlacementFailed(e.getMessage))
        }
        Effect.none
    }
  }

  val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case _ =>
        println("Event")
        State()
      //case Added(data) => state.copy((data :: state.history).take(5))
      //case Cleared     => State(Nil)
    }
  }
}
