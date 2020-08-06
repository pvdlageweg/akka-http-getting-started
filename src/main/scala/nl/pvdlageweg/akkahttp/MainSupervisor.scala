package nl.pvdlageweg.akkahttp

import akka.actor.typed.{Behavior, ChildFailed}
import akka.actor.typed.scaladsl.Behaviors

object MainSupervisor {
  // This is a sample of functional style actor
  // see docs: https://doc.akka.io/docs/akka/current/typed/style-guide.html

  /**
    * Actor builder method using scala apply(..)
    */
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      context.log.info("MainSupervisor started")

      // Start and watch AccountGroup actor
      val auctionActor = context.spawn(AuctionActor(), "AuctionActor")
      context.watch(auctionActor)

      // Start and watch HttpServer actor
      val httpServer = context.spawn(HttpServer(auctionActor), "HttpServer")
      context.watch(httpServer)

      // This actor only receives and handles signals from the monitored children
      // That is why it does not have protocol messages
      Behaviors.receiveSignal[Nothing] {

        // This is pattern matching anonymous function scala feature
        // see: https://danielwestheide.com/blog/the-neophytes-guide-to-scala-part-4-pattern-matching-anonymous-functions/
        case (_, ChildFailed(ref, cause)) =>
          context.log.warn("The child actor {} failed because {}", ref, cause.getMessage)
          Behaviors.same
      }
    }
}
