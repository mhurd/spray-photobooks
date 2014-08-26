package com.mhurd.repository

trait BookRepository {

  def findBookByIsbn(msg: FindByIsbn): Either[String, Book]

}
