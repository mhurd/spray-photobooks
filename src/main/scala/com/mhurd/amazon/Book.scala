package com.mhurd.amazon

import java.text.SimpleDateFormat
import java.util.Date
import spray.json.{NullOptions, DefaultJsonProtocol}
import scala.xml.{Elem, NodeSeq}

object BookJsonProtocol extends DefaultJsonProtocol with NullOptions {
  implicit val bookFormat = jsonFormat17(Book.apply)
}

case class Book(id: Option[String],
                isbn: Option[String],
                ean: Option[String],
                title: String,
                authors: Option[String],
                binding: Option[String],
                edition: Option[String],
                numberOfPages: Option[String],
                publicationDate: Option[String],
                publisher: Option[String],
                smallBookCover: Option[String],
                largeBookCover: Option[String],
                listPrice: Option[Int],
                lowestPrice: Option[Int],
                totalAvailable: Option[Int],
                lastPriceUpdateTimestamp: Option[Long],
                amazonPageUrl: Option[String]) {

  def noData = "- no data -"

  def noImage = "/assets/images/no-image.jpg"

  def smallBookCoverWithDefault: String =
    if (smallBookCover.isEmpty || smallBookCover.get == "") {
      noImage
    } else {
      smallBookCover.get
    }

  def largeBookCoverWithDefault: String =
    if (largeBookCover.isEmpty || largeBookCover.get == "") {
      noImage
    } else {
      largeBookCover.get
    }

  def displayableStringOption(option: Option[String]): String =
    option match {
      case None => noData
      case Some(text) => text
    }

  def displayableListPrice: String =
    listPrice match {
      case None => noData
      case Some(listPriceMatch) => "£" + (listPriceMatch / 100).toString
    }

  def displayableTotalAvailable: String =
    totalAvailable match {
      case None => "?"
      case Some(available) => available.toString
    }

  def displayableLowestPrice: String =
    lowestPrice match {
      case None => noData
      case Some(price) => "£" + (price / 100).toString
    }

  def displayableLastPriceUpdateTimestamp: String =
    lastPriceUpdateTimestamp match {
      case None => "unknown"
      case Some(timestamp) => Book.dateFormat.format(new Date(timestamp))
    }

  def toPrettyJson = {
    GetBookDetailsCmd.toPrettyJson(this)
  }

}

object Book {

  type Price = Option[Int]
  type TotalAvailable = Option[Int]
  type OfferSummary = (Price, TotalAvailable)

  val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")

  private def getOptionText(node: NodeSeq): Option[String] =
    node.headOption match {
      case None => None
      case Some(aNode) => Some(aNode.text)
    }

  private def getOptionInt(node: NodeSeq): Option[Int] =
    node.headOption match {
      case None => None
      case Some(aNode) => Some(aNode.text.toInt)
    }

  private def lowestPrice(offerSummaryNode: NodeSeq): Option[Int] = {
    val lowestUsedPrice = getPrice(offerSummaryNode, "LowestUsedPrice")
    val lowestNewPrice = getPrice(offerSummaryNode, "LowestNewPrice")
    lowestUsedPrice match {
      case None => lowestNewPrice
      case Some(uPrice) => lowestNewPrice match {
        case None => lowestUsedPrice
        case Some(nPrice) => if (uPrice <= nPrice) Some(uPrice) else Some(nPrice)
      }
    }
  }

  private def getPrice(offerSummaryNode: NodeSeq, priceName: String): Option[Int] = {
    offerSummaryNode \ priceName \ "Amount" text match {
      case "" => None
      case total => total match {
        case "0" => None
        case amount => Some(amount.toInt)
      }
    }
  }

  def availabilityFromAmazonXml(xml: NodeSeq): Option[OfferSummary] = {
    val offerSummaryNode = xml \ "Items" \ "Item" \ "OfferSummary" head
    val totalUsed = (offerSummaryNode \ "TotalUsed").text
    val totalNew = (offerSummaryNode \ "TotalNew").text
    val totalAvailable = if (totalUsed == "") 0 else totalUsed.toInt + (if (totalNew == "") 0 else totalNew.toInt)
    totalAvailable match {
      case 0 => Some((None, Some(0)))
      case _ => Some((lowestPrice(offerSummaryNode), Some(totalAvailable)))
    }
  }

  def fromAmazonXml(isbn: String, xml: Elem): Either[String, Book] =
    (xml \\ "Error").size match {
      case 0 => {
        val itemNode = xml \ "Items" \ "Item"
        val itemAttributesNode = itemNode \ "ItemAttributes"
        val authorsString = (itemAttributesNode \ "Author" map (f => f.text) mkString (", "))
        val authors = authorsString match {
          case "" => None
          case _ => Some(authorsString)
        }
        val amazonAvailability = availabilityFromAmazonXml(xml)
        Right(new Book(
          None,
          getOptionText(itemAttributesNode \ "ISBN"),
          getOptionText(itemAttributesNode \ "EAN"),
          itemAttributesNode \ "Title" text,
          authors,
          getOptionText(itemAttributesNode \ "Binding"),
          getOptionText(itemAttributesNode \ "Edition"),
          getOptionText(itemAttributesNode \ "NumberOfPages"),
          getOptionText(itemAttributesNode \ "PublicationDate"),
          getOptionText(itemAttributesNode \ "Publisher"),
          getOptionText(itemNode \ "MediumImage" \ "URL"),
          getOptionText(itemNode \ "LargeImage" \ "URL"),
          getOptionInt(itemAttributesNode \ "ListPrice" \ "Amount"),
          amazonAvailability match {
            case None => None
            case Some(some) => some._1
          },
          amazonAvailability match {
            case None => None
            case Some(some) => some._2
          },
          Some(System.currentTimeMillis()),
          getOptionText(itemNode \ "DetailPageURL")))
      }
      case _ => {
        Left("[" + (xml \\ "Error" \\ "Code" text) + "] " + (xml \\ "Error" \\ "Message" text))
      }
    }

}