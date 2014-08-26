package com.mhurd.repository.amazon

import akka.actor.{Actor, ActorSystem, Props}
import com.mhurd.repository.{Book, BookRepository, FindByIsbn}
import com.typesafe.config.Config

object AmazonRepositoryActor {
  def props(amazonClient: AmazonClient)(implicit system: ActorSystem, cfg: Config): Props = Props(new AmazonRepositoryActor(amazonClient))
}

class AmazonRepositoryActor(amazonClient: AmazonClient)(implicit system: ActorSystem, cfg: Config) extends Actor with BookRepository {

  def actorRefFactory = context

  def findBookByIsbn(msg: FindByIsbn): Either[String, Book] = {
    amazonClient.findBookByIsbn(msg.isbn) match {
      case Right(elem) => Book.fromAmazonXml(msg.isbn, elem) match {
        case Right(book) => Right(book)
        case Left(errorMsg) => Left(errorMsg)
      }
      case Left(errorMsg) => Left(errorMsg)
    }
  }

  def receive = {
    case msg: FindByIsbn => {
      sender ! findBookByIsbn(msg)
    }
    case _ => sender ! None
  }

}


