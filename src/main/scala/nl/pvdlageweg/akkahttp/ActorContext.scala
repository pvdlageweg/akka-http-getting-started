package nl.pvdlageweg.akkahttp

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

trait ActorContext {
  implicit val system = ActorSystem()
  implicit val executor: ExecutionContext = system.dispatcher
  //implicit val materializer: Materializer = Materializer()
}
