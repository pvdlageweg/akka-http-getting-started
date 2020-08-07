package nl.pvdlageweg.akkahttp

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object AuctionActor {
  case class Bid(userId: String, offer: Int)
  sealed trait AuctionCommands
  case class PlaceBid(bid: Bid, replyTo: ActorRef[BidsAcceptance]) extends AuctionCommands
  case class GetBids(replyTo: ActorRef[AuctionCommands]) extends AuctionCommands
  case class Bids(bids: List[Bid]) extends AuctionCommands

  sealed trait BidsAcceptance extends AuctionCommands
  case class BidRejected() extends BidsAcceptance
  case class BidAccepted() extends BidsAcceptance

  /**
    * Actor builder method
    */
  def apply(): Behavior[AuctionCommands] = apply(List.empty[Bid])

  def apply(bids: List[Bid]): Behavior[AuctionCommands] =
    Behaviors.setup { context =>
      context.log.info("AuctionActor started")

      Behaviors.receiveMessage {
        case PlaceBid(bid: Bid, replyTo: ActorRef[BidsAcceptance]) =>
          context.log.info(s"Bid recieved: $bid.userId, $bid.offer")

          val higherBidFound = bids.find(Bid => Bid.offer > bid.offer)
          higherBidFound match {
            case Some(bid) =>
              println(s"Found higher bid: ${bid.offer}")
              replyTo ! BidRejected()
              Behaviors.same
            case None =>
              println("No higher bid found")
              replyTo ! BidAccepted()
              apply(bids :+ bid)
          }
        case GetBids(replyTo) =>
          replyTo ! Bids(bids)
          Behaviors.same
        case _ =>
          Behaviors.unhandled
      }
    }
}
