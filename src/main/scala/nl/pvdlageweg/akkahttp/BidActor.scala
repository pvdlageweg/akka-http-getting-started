package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import nl.pvdlageweg.akkahttp.AuctionActor.Auction
import sun.security.krb5.KrbException.errorMessage

import scala.collection.Iterable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object BidActor {

  case class Bid(bidId: Int, auctionId: Int, offer: Float)
  case class BidRequest(auctionId: Int, offer: Float)

  sealed trait Command

  // Put a bid on an auction
  final case class RequestPlaceAuctionBid(bidRequest: BidRequest, replyTo: ActorRef[Response]) extends Command
  // Request a list of bids for an auction
  final case class RequestAuctionBids(auctionId: Int, replyTo: ActorRef[Response]) extends Command

  // Interface of actor outgoing messages (Responses)
  sealed trait Response

  final case class BidPlacementSuccessful() extends Response
  final case class BidPlacementFailed(error: String) extends Response
  final case class BidList(auctions: Iterable[Bid]) extends Response
  final case class BidListFetchingError(error: String) extends Response

  sealed trait Event extends Product with CborSerialized

  final case class BidCreated(bid: Bid) extends Event

  sealed trait State

  final case object Empty extends State
  private final case class InternalResponse(replyTo: ActorRef[Response], response: Response) extends Command
  def commandHandler(command: Command, context: ActorContext[Command], auctionDoa: AuctionDao, bidDao: BidDao)(implicit
      executionContext: ExecutionContext
  ): Effect[Event, State] =
    command match {
      case InternalResponse(replyTo, response) =>
        Effect.reply(replyTo)(response)
      case RequestAuctionBids(auctionId, replyTo) =>
        val bidsFuture: Future[Iterable[Bid]] = bidDao.ofAuction(auctionId)
        context.pipeToSelf(bidsFuture) {
          case Success(bidsIterable: Iterable[Bid]) => InternalResponse(replyTo, BidList(bidsIterable))
          case Failure(e)                           => InternalResponse(replyTo, BidListFetchingError(e.getMessage))
        }
        bidsFuture.onComplete {
          case Success(bidsIterable) => InternalResponse(replyTo, BidList(bidsIterable))
          case Failure(e)            => InternalResponse(replyTo, BidListFetchingError(e.getMessage))
        }
        Effect.none
      case RequestPlaceAuctionBid(bidRequest, replyTo) =>
        val auctionFuture: Future[Option[Auction]] = auctionDoa.read(bidRequest.auctionId)
        context.pipeToSelf(auctionFuture) {
          case Success(optionAuction) =>
            optionAuction match {
              case Some(_) =>
                println("Found auction")
                val bidsFuture = bidDao.ofAuction(bidRequest.auctionId)
                context.pipeToSelf(bidsFuture) {
                  case Success(bidsIterator) =>
                    val maxBid = bidsIterator.map(_.offer).max
                    println(s"max bix $maxBid")
                    if (bidRequest.offer <= maxBid) {
                      val errorMessage = s"Bid is less then current top bid of $maxBid"
                      InternalResponse(replyTo, BidPlacementFailed(errorMessage))
                    } else {
                      val saveBidFuture = bidDao.create(bidRequest)
                      saveBidFuture.onComplete {
                        case Success(savedBid) =>
                          println(s"Success!")
                          println(savedBid)
                          Effect.persist(bidRequest).thenReply(replyTo) { x: Bid => BidPlacementSuccessful() }
                        case Failure(e) => InternalResponse(replyTo, BidPlacementFailed(e.getMessage))
                      }
                      InternalResponse(replyTo, BidPlacementFailed("TODO"))
                    }
                  case Failure(e) =>
                    InternalResponse(replyTo, BidPlacementFailed(e.getMessage))
                    InternalResponse(replyTo, BidPlacementFailed("TODO2"))

                }
                Effect.none
              case None =>
                InternalResponse(replyTo, BidPlacementFailed("Auction not found"))
            }
          case Failure(e) => InternalResponse(replyTo, BidPlacementFailed(e.getMessage))
        }
        Effect.none
//        val bid = Bid(12, bidRequest.auctionId, bidRequest.offer)
//        Effect
//          .persist(BidCreated(bid))
//          .thenReply(replyTo) { st => BidPlacementSuccessful() }

    }

  def eventHandler(state: State, event: Event): State =
    state match {
      case _ => Empty // Ignore created events on this state
    }

  /**
    * Implementation of an actor that holds bank account state.
    * There is ONE ACTOR PER ACCOUNT discriminated by a specific accountId.
    *
    * This is an example of functional actor implementation
    */
  def apply(auctionDoa: AuctionDao, bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] =
    Behaviors.setup(context => eventSourcedBehavior(context, auctionDoa, bidDao))
//    {
//      EventSourcedBehavior.withEnforcedReplies[Command, Event, Response](
//        persistenceId = PersistenceId.ofUniqueId("bkkabka"),
//        emptyState = Empty,
//        commandHandler = (state, cmd) => commandHandler(cmd, context, auctionDoa, bidDao),
//        eventHandler = (state, evt) => eventHandler(state, evt)
//      )
//    })

  def eventSourcedBehavior(context: ActorContext[Command], auctionDoa: AuctionDao, bidDao: BidDao)(implicit
      executionContext: ExecutionContext
  ): EventSourcedBehavior[Command, Event, State] =
    EventSourcedBehavior(
      PersistenceId.ofUniqueId("bkkabka"),
      Empty,
      {
        case (_, cmd) =>
          println("handle cmd: " + cmd)
          commandHandler(cmd, context, auctionDoa, bidDao)
        case _ =>
          Effect.none
      },
      eventHandler
    )

  //
  //  case class BidRequest(auctionId: Int, offer: Float)
  //
  //  sealed trait Command
  //
  //  // Request a list of bids for an auction
  //  final case class RequestAuctionBids(auctionId: Int, replyTo: ActorRef[Response]) extends Command
  //
  //  // Put a bid on an auction
  //  final case class RequestPlaceAuctionBid(bid: BidRequest, replyTo: ActorRef[Response]) extends Command
  //
  //  // Interface of actor outgoing messages (Responses)
  //  trait Response
  //
  //  final case class BidList(auctions: Iterable[Bid]) extends Response with CborSerialized
  //  final case class BidListFetchingError(error: String) extends Response with CborSerialized
  //  final case class EmptyResponse() extends Response with CborSerialized
  //
  //  final case class BidPlacementSuccessful() extends Response with CborSerialized
  //  final case class BidPlacementFailed(error: String) extends Response with CborSerialized
  //
  //  //sealed trait Event
  //  //final case class State()
  //
  //  def apply(auctionDoa: AuctionDao, bidDao: BidDao)(implicit executionContext: ExecutionContext): Behavior[Command] = {
  //    EventSourcedBehavior[Command, Event, Response](
  //      persistenceId = PersistenceId.ofUniqueId("abc"),
  //      emptyState = EmptyResponse(),
  //      commandHandler = (state, command) => onCommand(command, auctionDoa, bidDao),
  //      eventHandler = (state, event) => eventHandler(event)
  //    )
  //  }
  //
  //  private def onCommand( command: Command, auctionDoa: AuctionDao, bidDao: BidDao)(implicit
  //      executionContext: ExecutionContext
  //  ): Effect[R, Response] = {
  //    command match {
  //      case RequestAuctionBids(auctionId, replyTo) =>
  //        val bidsFuture: Future[Iterable[Bid]] = bidDao.ofAuction(auctionId)
  //        bidsFuture.onComplete {
  //          case Success(bidsIterable) => replyTo ! BidList(bidsIterable)
  //          case Failure(e) => replyTo ! BidListFetchingError(e.getMessage)
  //        }
  //        Effect.none
  //      case RequestPlaceAuctionBid(bidRequest, replyTo) =>
  //        Effect.persist(bidRequest)
  //          .thenReply(replyTo) { x: Bid => BidPlacementSuccessful() }
  //
  //
  //
  //      //        val auctionFuture: Future[Option[Auction]] = auctionDoa.read(bidRequest.auctionId)
  //      //        auctionFuture.onComplete {
  //      //          case Success(optionAuction) =>
  //      //            optionAuction match {
  //      //              case Some(_) =>
  //      //                println("Found auction")
  //      //                val bidsFuture = bidDao.ofAuction(bidRequest.auctionId)
  //      //                bidsFuture.onComplete {
  //      //                  case Success(bidsIterator) =>
  //      //                    val maxBid = bidsIterator.map(_.offer).max
  //      //                    println(s"max bix $maxBid")
  //      //                    if (bidRequest.offer <= maxBid) {
  //      //                      val errorMessage = s"Bid is less then current top bid of $maxBid"
  //      //                      Effect.reply(replyTo)(BidPlacementFailed(errorMessage))
  //      //                    } else {
  //      //                      val saveBidFuture = bidDao.create(bidRequest)
  //      //                      saveBidFuture.onComplete {
  //      //                        case Success(savedBid) =>
  //      //                          println(s"Success!")
  //      //                          println(savedBid)
  //      //                          Effect.persist(bidRequest).thenReply(replyTo){x:Bid=>BidPlacementSuccessful()}
  //      //                        case Failure(e) => Effect.reply(replyTo){BidPlacementFailed(e.getMessage)}
  //      //                      }
  //      //                    }
  //      //
  //      //                  case Failure(e) => Effect.reply(replyTo)(BidPlacementFailed(e.getMessage))
  //      //                }
  //      //              case None =>
  //      //                Effect.reply(replyTo)(BidPlacementFailed("Auction not found"))
  //      //            }
  //      //          case Failure(e) => Effect.reply(replyTo)(BidPlacementFailed(e.getMessage))
  //      //        }
  //      //        Effect.none
  //    }
  //  }
  //
  //  val eventHandler:(evt: Response): Response = evt match {
  //    case wd: Withdrawed => update(wd)
  //    case dp: Deposited => update(dp)
  //    case _ => this  // Ignore created events on this state
  //  }
}
