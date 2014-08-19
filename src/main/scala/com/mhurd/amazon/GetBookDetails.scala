package com.mhurd.amazon

import spray.json._
import BookJsonProtocol._

object GetBookDetails {

  def toPrettyJson(book: Book) = {
    book.toJson.prettyPrint
  }

  def main(args: Array[String]) {
    if (args.length < 4) {
      println("Usage: <isbn> <access_key> <secret_key> <associate_tag>")
    } else {
      val isbn = args(0)
      val accessKey = args(1)
      val secretKey = args(2)
      val assocTag = args(3)
      val client = AmazonClient(accessKey, secretKey, assocTag)
      val xmlData = client.findByIsbn(isbn)
      val bookOption = Book.fromAmazonXml(isbn, xmlData)
      bookOption match {
        case None => println("Book not found!")
        case Some(book) => println(book.toJson.prettyPrint)
      }
      client.close()
    }
  }

}
