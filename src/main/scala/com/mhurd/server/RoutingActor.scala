package com.mhurd.server

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, ActorRef, Actor, Props}
import akka.util.Timeout
import com.mhurd.repository.amazon.AmazonClient
import com.mhurd.repository.{FindByIsbn, Book}
import spray.http.MediaTypes._
import spray.routing._
import akka.pattern.ask
import com.typesafe.config.Config

import scala.concurrent.Await
import scala.concurrent.duration._

object RoutingActor {
  def props(amazonRepositoryRouter: ActorRef, mongoRepositoryRouter: ActorRef)(implicit system: ActorSystem, cfg: Config): Props = Props(new RoutingActor(amazonRepositoryRouter, mongoRepositoryRouter))
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class RoutingActor(val amazonRepositoryRouter: ActorRef, val mongoRepositoryRouter: ActorRef)(implicit system: ActorSystem, cfg: Config) extends Actor with RoutingService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  implicit val akkaTimeout = Timeout(5, TimeUnit.SECONDS)
  private val awaitDuration = cfg.getInt("repository.timeout") seconds

  private def findBookByIsbn(router: ActorRef, isbn: String): Either[String, Book] = {
    val future = router ? FindByIsbn(isbn)
    Await.result(future.mapTo[Either[String, Book]], awaitDuration)
  }

  def findAmazonBookByIsbn(isbn: String): Either[String, Book] = {
    findBookByIsbn(amazonRepositoryRouter, isbn)
  }

  def findMongoBookByIsbn(isbn: String): Either[String, Book] = {
    findBookByIsbn(mongoRepositoryRouter, isbn)
  }

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

}

// this trait defines our service behavior independently from the service actor
trait RoutingService extends HttpService {

  def findAmazonBookByIsbn(isbn: String): Either[String, Book]

  def findMongoBookByIsbn(isbn: String): Either[String, Book]

  def myRoute = {
    path("mongo" / "isbn" / Segment) { isbn =>
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <code>
                  {
                    findMongoBookByIsbn(isbn) match {
                      case Right(book) => book.toString
                      case Left(errorMsg) => errorMsg
                    }
                  }
                </code>
              </body>
            </html>
          }
        }
      }
    } ~ path("amazon" / "isbn" / Segment ) { isbn =>
      get {
        respondWithMediaType(`text/html`) {

          complete {
            <html>
              <body>
                <code>
                  {
                    findAmazonBookByIsbn(isbn) match {
                      case Right(book) => book.toString
                      case Left(errorMsg) => errorMsg
                    }
                  }
                </code>
              </body>
            </html>
          }
        }
      }
    }
  }
}
