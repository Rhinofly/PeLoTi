package models

import com.mongodb.casbah.Imports._
import scala.concurrent.Future

trait PersonRepository {
  
  def getById(id: String): Future[Person]
  def getByLocation(location: Location, radius: Long): Future[List[Person]]
  def getByTime(start: Long, end: Option[Long]): Future[List[Person]]
  def getByLocationAndTime(location: Location, radius: Long, start: Long, end: Option[Long]): Future[List[Person]]
  def save(person: Person): Future[String]
}