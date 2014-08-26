package com.mhurd.repository.mongo

import akka.actor.{Actor, ActorSystem, Props}
import com.mhurd.repository.{Book, BookRepository, FindAll, FindByIsbn}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.typesafe.config.Config
import play.api.libs.json.Json

object MongoRepositoryActor {
  def props()(implicit system: ActorSystem, cfg: Config): Props = Props(new MongoRepositoryActor())
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MongoRepositoryActor()(implicit system: ActorSystem, cfg: Config) extends Actor with BookRepository {

  // property is in the form host:port,host:port
  private val replicaSetServers = getServerAddresses(cfg.getString("mongodb.servers"))
  private val username = cfg.getString("mongodb.username")
  private val password = cfg.getString("mongodb.password").toCharArray
  private val database = cfg.getString("mongodb.database")
  private val mongoClient = MongoClient(replicaSetServers, replicaSetServers.map(_ => MongoCredential(username, database, password)))
  private val mongoDB = mongoClient.getDB(database)
  private val booksCollection = new MongoCollection(mongoDB.getCollection("books"))

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  def receive = {
    case FindByIsbn(isbn) => sender ! findBookByIsbn(isbn)
    case FindAll => sender ! findAllBooks
    case _ => sender ! Left("Message not recognised.")
  }

  def findBookByIsbn(isbn: String): Either[String, Book] = {
    val query = MongoDBObject("isbn" -> isbn)
    system.log.info("repository find books called with query: " + query)
    val dbBooks = for {
      bookJson <- booksCollection.find(query)
    } yield {
      Book.BookFormat.reads(Json.parse(bookJson.toString)).get
    }
    dbBooks.toList match {
      case h :: tail => Right(h)
      case Nil => Left(s"Books with isbn ${isbn} not found in MongoDB")
    }
  }

  def findAllBooks: Either[String, List[Book]] = {
    val dbBooks = for {
      bookJson <- booksCollection.find().sort(Map("title" -> 1))
    } yield {
      Book.BookFormat.reads(Json.parse(bookJson.toString)).get
    }
    Right(dbBooks.toList)
  }

  private def getServerAddresses(addresses: String): List[ServerAddress] = {
    addresses match {
      case "" => throw new IllegalArgumentException("No MongoDB Servers configured!")
      case s => {
        val servers = s.split(",")
        (for {
          (host, port) <- servers.map(_.split(":")(0)).zip(servers.map(_.split(":")(1)))
        } yield {
          new ServerAddress(host, port.toInt)
        }).toList
      }
    }
  }

}


