package models.repository

import com.mongodb.casbah.Imports._
import scala.concurrent.Future
import models.Location
import models.Person

trait PersonRepository {
  def getById(id: String): Future[Person]
  def getByLocation(location: Location, radius: Long): Future[List[Person]]
  def getByTime(start: Long, end: Option[Long]): Future[List[Person]]
  def getByLocationAndTime(location: Location, radius: Long, start: Long, end: Option[Long]): Future[List[Person]]
  def save(person: Person): Future[String]
  def saveExtra(id: String, list: List[(String, String)]): Future[Person]
}