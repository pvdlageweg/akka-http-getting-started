package nl.pvdlageweg.akkahttp
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import nl.pvdlageweg.akkahttp.AuctionActor._
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2, _}
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
object AuctionApi {
  def apply(actor: ActorRef[Command], system: ActorSystem[_]) = new AuctionApi(actor, system)
}

class AuctionApi(private val actor: ActorRef[Command], private val system: ActorSystem[_]) {

  // Needed for ask pattern and Futures
  implicit val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  implicit val scheduler: Scheduler = system.scheduler
  implicit val ec: ExecutionContextExecutor = system.executionContext
  //implicit val bidFormat: RootJsonFormat[Bid] = jsonFormat3(Bid)
  implicit val auctionFormat: RootJsonFormat[Auction] = jsonFormat2(Auction)
  //implicit val bidsFormat: RootJsonFormat[Bids] = jsonFormat1(Bids)
  implicit val auctionsFormat: RootJsonFormat[AuctionList] = jsonFormat1(AuctionList)
  val routes: Route = concat(
    auctions(),
    auction()
  )

  // GET /auctions
  // returns AuctionList(..)
  private def auctions(): Route =
    path("auctions") {
      get {
        val future = actor.ask[Response](replyTo => RequestAuctionList(replyTo))

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
        val future = actor.ask[Response](replyTo => RequestAuction(auctionId.toInt, replyTo))

        onSuccess(future) {
          case ExistingAuction(auction) =>
            complete(StatusCodes.OK, auction)
          case AuctionNotFound(auctionId) =>
            complete(StatusCodes.NotFound)
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
