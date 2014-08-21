package com.mhurd.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.io.IO
import akka.util.Timeout
import com.mhurd.amazon.AmazonClient
import spray.can.Http
import akka.event.Logging

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("spray-photobooks")

  val log = Logging(system, getClass)

  val accessKey = args(0)
  val secretKey = args(1)
  val assocTag = args(2)

  // create and start our service actor
  val service = system.actorOf(ExampleServiceActor.props(AmazonClient(accessKey, secretKey, assocTag)), "spray-photobooks-rest-api")

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)

}
