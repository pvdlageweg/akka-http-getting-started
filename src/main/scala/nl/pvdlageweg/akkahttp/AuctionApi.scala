package nl.pvdlageweg.akkahttp
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import nl.pvdlageweg.akkahttp.AuctionActor.{AuctionCommands, Bid, Bids, GetBids, PlaceBid}
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.concurrent.duration._
object AuctionApi {
  def apply(actor: ActorRef[AuctionCommands], system: ActorSystem[_]) = new AuctionApi(actor, system)
}

class AuctionApi(private val actor: ActorRef[AuctionCommands], private val system: ActorSystem[_]) {

  // Needed for ask pattern and Futures
  implicit val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  implicit val scheduler = system.scheduler
  implicit val ec = system.executionContext
  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)
  val routes: Route = concat(
    auction()
  )

  // GET /auction
  private def auction(): Route =
    path("auctions") {
      concat(
        put {
          parameter("bid".as[Int], "user") { (bid, user) =>
            // place a bid, fire-and-forget
            actor ! PlaceBid(Bid(user, bid))
            complete(StatusCodes.Accepted, "bid placed")
          }
        },
        get {
          implicit val timeout: Timeout = 5.seconds

          // query the actor for the current auction state
          implicit val scheduler: Scheduler = system.scheduler
          val bidsFuture: Future[Bids] = (actor ? GetBids).mapTo[Bids]
          complete(bidsFuture)
        }
      )
    }

}
