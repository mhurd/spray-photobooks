package com.mhurd.server

import akka.actor.ActorSystem
import akka.event.Logging
import akka.io.IO
import akka.routing.RoundRobinRouter
import com.mhurd.repository.amazon.{AmazonClient, AmazonRepositoryActor}
import com.mhurd.repository.mongo.MongoRepositoryActor
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.io.ServerSSLEngineProvider

object Main extends App {

  // we need an ActorSystem to host our application in
  implicit val akkaSystem = ActorSystem("spray-photobooks")

  val log = Logging(akkaSystem, getClass)

  implicit val cfg = ConfigFactory.load();

  val accessKey = args(0)
  val secretKey = args(1)
  val assocTag = args(2)

  val amazonRepositoryRouter = akkaSystem.actorOf(
    AmazonRepositoryActor.props(AmazonClient(accessKey, secretKey, assocTag)).withRouter(RoundRobinRouter(numberOfActors())), name = "amazonRepositoryRouter")

  val mongoRepositoryRouter = akkaSystem.actorOf(
    MongoRepositoryActor.props().withRouter(RoundRobinRouter(numberOfActors())), name = "mongoRepositoryRouter")
  // create and start our service actor

  val serviceRouterActor = akkaSystem.actorOf(RoutingActor.props(
    amazonRepositoryRouter,
    mongoRepositoryRouter), name = "serviceRouterActor")

  def numberOfActors(): Int = {
    val parallelismCoefficient = 75 // 1..100, lower for CPU-bound, higher for IO-bound
    val number = Runtime.getRuntime.availableProcessors * 100 / (100 - parallelismCoefficient)
    log.info("Using " + number + " actors per router...")
    number
  }

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(serviceRouterActor, interface = "localhost", port = 8080)

  sys.addShutdownHook({
    log.info("Shutting down akka...")
    akkaSystem.shutdown
  })

}
