package model

import scala.collection.mutable

case class Name(id: Int,	primaryName: String, movies: mutable.ListBuffer[Movie] = new mutable.ListBuffer[Movie]){
  var baconDistance: Int = -1
  override def hashCode() = id
}

