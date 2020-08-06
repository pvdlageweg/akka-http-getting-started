package nl.pvdlageweg.akkahttp

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import nl.pvdlageweg.akkahttp.AuctionActor.AuctionCommands

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object HttpServer {
  def apply(actor: ActorRef[AuctionCommands]) =
    Behaviors.setup[Done] { context =>
      context.log.info(s"Http actor  started")

      // Needed for Http server and Futures

      // Akka http uses the classic actor system api
      implicit val system = context.system
      implicit val ec: ExecutionContext = context.system.executionContext

      val api = AuctionApi(actor, system)
      val serverBinding: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(api.routes)

      serverBinding.onComplete { // Another pattern matching anonymous function
        case Success(bound) =>
          context.log.info(s"Server online at http://${bound.localAddress}")
        case Failure(ex) =>
          context.log.error(s"Server could not start", ex)
        case _ =>
          println("Some thing else")
      }

//      serverBinding
//        .flatMap(_.unbind()) // trigger unbinding from the port

      // Return a behavior that terminates the actor if the serverBinding completes
      // that is the akka Http server cannot bind a tcp endpoint with failure.
      Behaviors.receiveMessage {
        case Done =>
          Behaviors.stopped
      }
    }

}
