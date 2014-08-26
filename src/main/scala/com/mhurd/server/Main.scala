package com.mhurd.server

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import com.mhurd.repository.amazon.{AmazonRepositoryActor, AmazonClient}
import com.mhurd.repository.mongo.MongoRepositoryActor
import com.typesafe.config.{ConfigFactory, Config}
import spray.can.Http
import akka.event.Logging

object Main extends App {

  // we need an ActorSystem to host our application in
  implicit val akkaSystem = ActorSystem("spray-photobooks")

  val log = Logging(akkaSystem, getClass)

  implicit val cfg = ConfigFactory.load();

  val accessKey = args(0)
  val secretKey = args(1)
  val assocTag = args(2)

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  def numberOfActors(): Int = {
    val parallelismCoefficient = 75 // 1..100, lower for CPU-bound, higher for IO-bound
    val number = Runtime.getRuntime.availableProcessors * 100 / (100 - parallelismCoefficient)
    log.info("Using " + number + " actors per router...")
    number
  }

  val amazonRepositoryRouter = akkaSystem.actorOf(
    AmazonRepositoryActor.props(AmazonClient(accessKey, secretKey, assocTag)).withRouter(RoundRobinRouter(numberOfActors())), name = "amazonRepositoryRouter")

  val mongoRepositoryRouter = akkaSystem.actorOf(
    MongoRepositoryActor.props().withRouter(RoundRobinRouter(numberOfActors())), name = "mongoRepositoryRouter")

  // create and start our service actor
  val serviceRouterActor = akkaSystem.actorOf(RoutingActor.props(
    amazonRepositoryRouter,
    mongoRepositoryRouter), name = "serviceRouterActor")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(serviceRouterActor, interface = "localhost", port = 8080)

  sys.addShutdownHook({
    log.info("Shutting down akka...")
    akkaSystem.shutdown
  })

}
