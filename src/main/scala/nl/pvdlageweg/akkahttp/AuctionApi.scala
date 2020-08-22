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

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

object AuctionApi {
  def apply(actor: ActorRef[AuctionCommands], system: ActorSystem[_]) = new AuctionApi(actor, system)
}

class AuctionApi(private val actor: ActorRef[AuctionCommands], private val system: ActorSystem[_]) {

  // Needed for ask pattern and Futures
  implicit val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  implicit val scheduler: Scheduler = system.scheduler
  implicit val ec: ExecutionContextExecutor = system.executionContext
  implicit val bidFormat: RootJsonFormat[Bid] = jsonFormat2(Bid)
  implicit val bidsFormat: RootJsonFormat[Bids] = jsonFormat1(Bids)
  val routes: Route = concat(
    auction()
  )

  // GET /auction
  private def auction(): Route =
    path("auctions") {
      concat(
        put {
          parameter("bid".as[Int], "user") { (bid, user) =>
            onComplete(actor.ask[BidsAcceptance](ref => PlaceBid(Bid(user, bid), ref)).mapTo[BidsAcceptance]) {
              case Success(bidResult) =>
                bidResult match {
                  case BidAccepted() => complete(StatusCodes.Accepted, "bid placed")
                  case BidRejected() => complete(StatusCodes.Conflict, "bid rejected")
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
    }

}
