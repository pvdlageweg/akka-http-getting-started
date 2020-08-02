package nl.pvdlageweg.akkahttp
import akka.actor
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object Main {
  final case class Bid(userId: String, offer: Int)
  sealed trait AuctionCommands
  final case class PlaceBid(bid: Bid) extends AuctionCommands
  final case class GetBids(replyTo: ActorRef[AuctionCommands]) extends AuctionCommands
  final case class Bids(bids: List[Bid]) extends AuctionCommands

  def apply(): Behavior[AuctionCommands] = apply(List.empty[Bid])

  def apply(bids: List[Bid]): Behavior[AuctionCommands] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case PlaceBid(bid: Bid) =>
          context.log.info(s"Bid complete: $bid.userId, $bid.offer")
          apply(bids :+ bid)
        case GetBids(replyTo) =>
          replyTo ! Bids(bids)
          Behaviors.same
      }
    }

  // these are from spray-json
  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)

  def main(args: Array[String]) {
    implicit val system: ActorSystem[AuctionCommands] =
      ActorSystem[AuctionCommands](apply(), "Action")
    implicit val materializer: Materializer = Materializer(system)
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor =
      system.executionContext

    val route =
      path("auction") {
        concat(
          put {
            parameter("bid".as[Int], "user") { (bid, user) =>
              // place a bid, fire-and-forget
              system ! PlaceBid(Bid(user, bid))
              complete((StatusCodes.Accepted, "bid placed"))
            }
          },
          get {
            implicit val timeout: Timeout = 5.seconds

            // query the actor for the current auction state
            implicit val scheduler: Scheduler = system.scheduler
            val bids: Future[Bids] = (system ? GetBids).mapTo[Bids]
            complete(bids)
          }
        )
      }

    implicit val clasic: actor.ActorSystem = system.toClassic
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
