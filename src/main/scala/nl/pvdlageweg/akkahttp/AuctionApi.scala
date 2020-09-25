package nl.pvdlageweg.akkahttp
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import nl.pvdlageweg.akkahttp.AuctionActor._
import nl.pvdlageweg.akkahttp.BidActor._
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2, _}
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object AuctionApi {
  def apply(
      auctionActor: ActorRef[AuctionActor.Command],
      bidActor: ActorRef[BidActor.Command],
      system: ActorSystem[_]
  ) =
    new AuctionApi(auctionActor, bidActor, system)
}

class AuctionApi(
    private val auctionActor: ActorRef[AuctionActor.Command],
    private val bidActor: ActorRef[BidActor.Command],
    private val system: ActorSystem[_]
) {

  // Needed for ask pattern and Futures
  implicit val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  implicit val scheduler: Scheduler = system.scheduler
  implicit val ec: ExecutionContextExecutor = system.executionContext
  implicit val bidFormat: RootJsonFormat[Bid] = jsonFormat3(Bid)
  implicit val auctionFormat: RootJsonFormat[Auction] = jsonFormat2(Auction)
  //implicit val bidsFormat: RootJsonFormat[List[Bid]] = jsonFormat2(List[Bid])
  implicit val auctionsFormat: RootJsonFormat[AuctionList] = jsonFormat1(AuctionList)
  val routes: Route = concat(
    auctions(),
    auction(),
    bids(),
    placeBid()
  )

  // GET /auctions
  // returns AuctionList(..)
  private def auctions(): Route =
    path("auctions") {
      get {
        val future = auctionActor.ask[AuctionActor.Response](replyTo => RequestAuctionList(replyTo))

        onSuccess(future) {
          case AuctionList(auctions) =>
            complete(StatusCodes.OK, auctions)
        }
      }
    }

  // GET /auction/id
  // returns Auction(..)
  private def auction(): Route =
    path("auction" / Segment) { auctionId =>
      get {
        val future = auctionActor.ask[AuctionActor.Response](replyTo => RequestAuction(auctionId.toInt, replyTo))

        onSuccess(future) {
          case ExistingAuction(auction) =>
            complete(StatusCodes.OK, auction)
          case AuctionNotFound(auctionId) =>
            complete(StatusCodes.NotFound)
        }
      }
    }

  // GET /bids/auctionId
  // returns Auction(..)
  private def bids(): Route =
    path("bids" / Segment) { auctionId =>
      get {
        val future = bidActor.ask[BidActor.Response](replyTo => RequestAuctionBids(auctionId.toInt, replyTo))

        onSuccess(future) {
          case BidList(bids) =>
            complete(StatusCodes.OK, bids)
          case BidListFetchingError(error) =>
            complete(StatusCodes.NotFound, error)
        }
      }
    }

  // POST /bids/auctionId/place/offer
  private def placeBid(): Route =
    path("bids" / Segment / "place" / Segment) { (auctionId, offer) =>
      post {
        val future = bidActor.ask[BidActor.Response](replyTo =>
          RequestPlaceAuctionBid(BidRequest(auctionId.toInt, offer.toFloat), replyTo)
        )
        onSuccess(future) {
          case BidPlacementSuccessful() =>
            complete(StatusCodes.OK)
          case BidPlacementFailed(error) =>
            complete(StatusCodes.BadRequest, error)
        }
      }
    }

  /*
  // GET /auction
  private def auction(): Route =
    concat(
      path("auctions") {
        concat(
          put {
            parameter("auction".as[Int], "bid".as[Int], "user") { (auction, bid, user) =>
              onComplete(
                actor.ask[BidsResponse](ref => PlaceBid(Bid(auction, user, bid), ref)).mapTo[BidsResponse]
              ) {
                case Success(bidResult) =>
                  bidResult match {
                    case BidAccepted()      => complete(StatusCodes.Accepted, "bid placed")
                    case BidRejected(error) => complete(StatusCodes.Conflict, s"bid rejected: ${error}")
                  }
                case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
            }
          },
          get {
            // query the actor for the current auction state
            implicit val scheduler: Scheduler = system.scheduler
            val bidsFuture: Future[Bids] = (actor ? GetBids).mapTo[Bids]
            complete(bidsFuture)
          }
        )
      },
      path("auctionlist") {
        get {

          val auctionListFuture: Future[AuctionList] = (actor ? RequestAuctionList).mapTo[AuctionList]
          complete(auctionListFuture)
        }
      }
    )
   */
}
