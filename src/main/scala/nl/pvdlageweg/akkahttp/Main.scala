package nl.pvdlageweg.akkahttp
import akka.actor.typed.ActorSystem

object Main {
  def main(args: Array[String]) {
    ActorSystem[Nothing](MainSupervisor(), "Auction")
  }
}
