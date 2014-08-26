package com.mhurd.repository.mongo

import akka.actor.{Actor, ActorSystem, Props}
import com.mhurd.repository.{Book, BookRepository, FindByIsbn}
import com.mongodb.ServerAddress
import com.mongodb.casbah.Imports._
import com.typesafe.config.Config

object MongoRepositoryActor {
  def props()(implicit system: ActorSystem, cfg: Config): Props = Props(new MongoRepositoryActor())
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MongoRepositoryActor()(implicit system: ActorSystem, cfg: Config) extends Actor with BookRepository {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

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

  // property is in the form host:port,host:port
  private val replicaSetServers = getServerAddresses(cfg.getString("mongodb.servers"))
  private val username = cfg.getString("mongodb.username")
  private val password = cfg.getString("mongodb.password").toCharArray
  private val database = cfg.getString("mongodb.database")

  private val mongoClient = MongoClient(replicaSetServers, replicaSetServers.map(_ => MongoCredential(username, database, password)))
  private val mongoDB = mongoClient.getDB(database)
  private val booksCollection = mongoDB.getCollection("books")

  def findBookByIsbn(msg: FindByIsbn): Either[String, Book] = {
    val isbn = msg.isbn
    val q = MongoDBObject("isbn" -> isbn)
    system.log.info("repository find books called with query: " + q)
    Left(s"No book matching $isbn found")
  }

  def receive = {
    case msg: FindByIsbn => {
      sender ! findBookByIsbn(msg)
    }
    case _ => sender ! None
  }

}


