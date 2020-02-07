package model

import scala.collection.mutable

case class Movie(
  id: Int,
  primaryTitle: String,
  originalTitle: String,
  isAdult: Boolean,
  startYear: Option[Int],
  endYear: Option[Int],
  runtimeMinutes: Option[Int],
  genres: Int,
  principals: mutable.ListBuffer[Name] = new mutable.ListBuffer[Name]
){
  override def hashCode(): Int = id
}
