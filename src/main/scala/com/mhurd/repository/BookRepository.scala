package com.mhurd.repository

trait BookRepository {

  def findAllBooks: Either[String, List[Book]]

  def findBookByIsbn(isbn: String): Either[String, Book]

}
