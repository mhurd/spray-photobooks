package com.mhurd.amazon

import akka.actor.ActorSystem
import spray.json._
import BookJsonProtocol._

object GetBookDetailsCmd {

  def toPrettyJson(book: Book) = {
    book.toJson.prettyPrint
  }

  def main(args: Array[String]) {

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("spray-photobooks")

    if (args.length < 4) {
      println("Usage: <isbn> <access_key> <secret_key> <associate_tag>")
    } else {
      val isbn = args(0)
      val accessKey = args(1)
      val secretKey = args(2)
      val assocTag = args(3)
      val client = AmazonClient(accessKey, secretKey, assocTag)
      client.findByIsbn(isbn) match {
        case Left(error) => println(error)
        case Right(elem) => {
          Book.fromAmazonXml(isbn, elem) match {
            case Left(error) => println(error)
            case Right(book) => println(book.toJson.prettyPrint)
          }
        }
      }
      system.shutdown()
    }
  }

}
