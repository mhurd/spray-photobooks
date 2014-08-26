package com.mhurd.repository.amazon

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.apache.commons.codec.binary.Base64
import spray.can.Http
import spray.http.HttpMethods._
import spray.http.StatusCodes.Success
import spray.http.{HttpRequest, HttpResponse, Uri}

import scala.Predef._
import scala.collection.immutable.SortedMap
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.xml.Elem

trait AmazonClient {

  def findBookByKeywords(keywords: List[String]): Either[String, Elem]

  def findBookByIsbn(isbn: String): Either[String, Elem]

  def findOfferSummaryByIsbn(isbn: String): Either[String, Elem]

}

object AmazonClient {

  def apply(accessKey: String, secretKey: String, associateTag: String)(implicit system: ActorSystem): AmazonClient = {
    new AmazonImpl(accessKey, secretKey, associateTag)
  }

}

private class AmazonImpl(private val accessKey: String, private val secretKey: String, private val associateTag: String)(implicit system: ActorSystem) extends AmazonClient {

  private implicit val timeout: Timeout = 5.seconds

  private val awaitDuration: Duration = 5.seconds

  // Amazon API Constants
  private val AMAZON_API_VERSION = "2011-08-01"
  private val AMAZON_SERVICE = "AWSECommerceService"
  private val AMAZON_API_HOST = "ecs.amazonaws.co.uk"
  private val AMAZON_API_REQUEST_URI = "/onca/xml"

  // Request signing
  private val SHA_256 = "HmacSHA256"
  private val UTF8_CHARSET = "UTF-8"
  private val encoder = new Base64(0)
  private val ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  private val secretKeySpec: SecretKeySpec = new SecretKeySpec(secretKey.getBytes(UTF8_CHARSET), SHA_256)
  private val mac = Mac.getInstance(SHA_256)
  mac.init(secretKeySpec)

  // Base arguments for the Amazon API request
  private val BASIC_ARGUMENTS = SortedMap(
    "Service" -> AMAZON_SERVICE,
    "Version" -> AMAZON_API_VERSION,
    "AWSAccessKeyId" -> accessKey,
    "AssociateTag" -> associateTag,
    "SearchIndex" -> "Books",
    "Condition" -> "All",
    "Offer" -> "All",
    "ResponseGroup" -> "ItemAttributes,OfferSummary,Images"
  )

  def findBookByKeywords(keywords: List[String]): Either[String, Elem] = {
    request(SortedMap("Operation" -> "ItemSearch", "Keywords" -> keywords.mkString("+")))
  }

  def findBookByIsbn(isbn: String): Either[String, Elem] = {
    request(SortedMap("Operation" -> "ItemLookup", "ItemId" -> isbn, "IdType" -> "ISBN"))
  }

  private def request(arguments: SortedMap[String, String]): Either[String, Elem] = {
    // execution context for future transformation below
    val result = for {
      response <- IO(Http).ask(HttpRequest(GET, Uri(getSignedUrl(arguments)))).mapTo[HttpResponse]
    } yield {
      response
    }
    val response = Await.result(result, awaitDuration)
    response.status match {
      case Success(_) => Right(scala.xml.XML.loadString(response.entity.data.asString))
      case _ => Left(response.status.defaultMessage)
    }
  }

  private def getSignedUrl(arguments: SortedMap[String, String]): String = {
    val args = mergeArguments(arguments)
    val toSign = "GET" + "\n" + AMAZON_API_HOST + "\n" + AMAZON_API_REQUEST_URI + "\n" + args
    val hmacResult = hmac(toSign)
    val sig = percentEncodeRfc3986(hmacResult)
    "http://" + AMAZON_API_HOST + AMAZON_API_REQUEST_URI + "?" + args + "&Signature=" + sig
  }

  private def hmac(stringToSign: String): String = {
    val data = stringToSign.getBytes(UTF8_CHARSET)
    val rawHmac = mac.doFinal(data)
    new String(encoder.encode(rawHmac))
  }

  private def mergeArguments(arguments: SortedMap[String, String]): String = {
    val mergedArguments: SortedMap[String, String] = BASIC_ARGUMENTS ++ arguments + ("Timestamp" -> getIso8601TimestampString)
    val f = {
      (p: (String, String)) => percentEncodeRfc3986(p._1) + "=" + percentEncodeRfc3986(p._2)
    }
    mergedArguments.map(f).mkString("&")
  }

  private def getIso8601TimestampString: String = {
    val format = new java.text.SimpleDateFormat(ISO_8601_TIMESTAMP_FORMAT)
    format.format(new java.util.Date())
  }

  private def percentEncodeRfc3986(s: String): String = {
    URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20")
  }

  def findOfferSummaryByIsbn(isbn: String): Either[String, Elem] = {
    request(SortedMap("ResponseGroup" -> "OfferSummary", "Operation" -> "ItemLookup", "ItemId" -> isbn, "IdType" -> "ISBN"))
  }

}
