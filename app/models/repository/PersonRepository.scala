package models.repository

import scala.concurrent.Future

import models.service.Location
import models.service.Person

trait PersonRepository {
  def getById(id: String): Future[Option[Person]]
  def getByLocation(location: Location, radius: Long): Future[List[Person]]
  def getByTime(start: Long, end: Option[Long]): Future[List[Person]]
  def getByLocationAndTime(location: Location, radius: Long, start: Long, end: Option[Long]): Future[List[Person]]
  def save(person: Person): Future[Person]
}