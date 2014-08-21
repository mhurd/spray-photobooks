package com.mhurd.server

import akka.actor.{Actor, Props}
import com.mhurd.amazon.{AmazonClient, Book}
import spray.http.MediaTypes._
import spray.routing._

object ExampleServiceActor {
  def props(amazonClient: AmazonClient): Props = Props(new ExampleServiceActor(amazonClient))
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ExampleServiceActor(val amazonClient: AmazonClient) extends Actor with ExampleService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute(amazonClient))
}

// this trait defines our service behavior independently from the service actor
trait ExampleService extends HttpService {

  def myRoute(amazonClient: AmazonClient) = {
    pathPrefix("isbn" / Segment ) { isbn =>
      get {
        respondWithMediaType(`text/html`) {
          // XML is marshaled to `text/xml` by default, so we simply override here
          val str = amazonClient.findByIsbn(isbn) match {
            case Left(error) => error
            case Right(elem) => {
              Book.fromAmazonXml(isbn, elem) match {
                case Left(error) => error
                case Right(book) => book.toPrettyJson
              }
            }
          }
          complete {
            <html>
              <body>
                <code>
                  {str}
                </code>
              </body>
            </html>
          }
        }
      }
    }
  }
}
