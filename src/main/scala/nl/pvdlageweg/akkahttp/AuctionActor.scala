package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object AuctionActor {
  case class Bid(userId: String, offer: Int)
  sealed trait AuctionCommands
  case class PlaceBid(bid: Bid) extends AuctionCommands
  case class GetBids(replyTo: ActorRef[AuctionCommands]) extends AuctionCommands
  case class Bids(bids: List[Bid]) extends AuctionCommands

  /**
    * Actor builder method
    */
  def apply(): Behavior[AuctionCommands] = apply(List.empty[Bid])

  def apply(bids: List[Bid]): Behavior[AuctionCommands] =
    Behaviors.setup { context =>
      context.log.info("AccountGroup started")

      Behaviors.receiveMessage {
        case PlaceBid(bid: Bid) =>
          context.log.info(s"Bid complete: $bid.userId, $bid.offer")
          apply(bids :+ bid)
        case GetBids(replyTo) =>
          replyTo ! Bids(bids)
          Behaviors.same
        case _ =>
          Behaviors.unhandled
      }
    }
}
