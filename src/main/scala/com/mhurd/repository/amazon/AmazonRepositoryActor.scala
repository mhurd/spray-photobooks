package com.mhurd.repository.amazon

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.mhurd.repository.{Book, BookRepository, FindByIsbn}
import com.typesafe.config.Config

object AmazonRepositoryActor {
  def props(amazonClient: AmazonClient)(implicit system: ActorSystem, cfg: Config): Props = Props(new AmazonRepositoryActor(amazonClient))
}

class AmazonRepositoryActor(amazonClient: AmazonClient)(implicit system: ActorSystem, cfg: Config) extends Actor with BookRepository with ActorLogging {

  def actorRefFactory = context

  def receive = {
    case msg: FindByIsbn => {
      val r = findBookByIsbn(msg)
      r match {
        case Right(book) => {
          log.info("Retrieved book details from Amazon for isbn {}", msg.isbn)
        }
        case Left(errorMsg) => {
          log.info("Unable to retrieve book details from Amazon for isbn {} : {}", msg.isbn, errorMsg)
        }
      }
      sender ! r
    }
    case _ => sender ! None
  }

  def findBookByIsbn(msg: FindByIsbn): Either[String, Book] = {
    amazonClient.findBookByIsbn(msg.isbn) match {
      case Right(elem) => Book.fromAmazonXml(msg.isbn, elem) match {
        case Right(book) => Right(book)
        case Left(errorMsg) => Left(errorMsg)
      }
      case Left(errorMsg) => Left(errorMsg)
    }
  }

}


