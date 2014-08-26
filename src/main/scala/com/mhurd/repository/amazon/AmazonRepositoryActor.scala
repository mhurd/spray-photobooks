package com.mhurd.repository.amazon

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.mhurd.repository.{Book, BookRepository, FindAll, FindByIsbn}
import com.typesafe.config.Config

object AmazonRepositoryActor {
  def props(amazonClient: AmazonClient)(implicit system: ActorSystem, cfg: Config): Props = Props(new AmazonRepositoryActor(amazonClient))
}

class AmazonRepositoryActor(amazonClient: AmazonClient)(implicit system: ActorSystem, cfg: Config) extends Actor with BookRepository with ActorLogging {

  def actorRefFactory = context

  def receive = {
    case FindByIsbn(isbn) => {
      val r = findBookByIsbn(isbn)
      r match {
        case Right(book) => {
          log.info("Retrieved book details from Amazon for isbn {}", isbn)
        }
        case Left(errorMsg) => {
          log.info("Unable to retrieve book details from Amazon for isbn {} : {}", isbn, errorMsg)
        }
      }
      sender ! r
    }
    case msg: FindAll => sender ! findAllBooks
    case _ => sender ! Left("Message not recognised.")
  }

  def findBookByIsbn(isbn: String): Either[String, Book] = {
    amazonClient.findBookByIsbn(isbn) match {
      case Right(elem) => Book.fromAmazonXml(isbn, elem) match {
        case Right(book) => Right(book)
        case Left(errorMsg) => Left(errorMsg)
      }
      case Left(errorMsg) => Left(errorMsg)
    }
  }

  def findAllBooks: Either[String, List[Book]] = {
    Left("Operation not supported by Amazon Repository...")
  }

}


