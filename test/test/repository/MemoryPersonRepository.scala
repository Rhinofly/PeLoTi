package test.repository

import scala.annotation.migration
import scala.collection.immutable.{Map => iMap}
import scala.collection.mutable.Map
import scala.concurrent.Future

import models.repository.PersonRepository
import models.service.Location
import models.service.Person
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class MemoryPersonRepository extends PersonRepository {
  
  //TODO Put in configuration => Default timespan
  val time = 1000 * 60 * 10
  
  val list = Map("1" -> Person(Location(54.2, 5.2), 156643413L, Option("1")), 
                 "2" -> Person(Location(54.3, 5.2), 156643413L, Option("1"), iMap("key" -> "value")), 
                 "3" -> Person(Location(54.2, 5.2), 156643417L, Option("1")), 
                 "4" -> Person(Location(54.2, 5.3), 156643413L, Option("1")))
  
  override def getById(id: String): Future[Option[Person]] = Future(Option(list(id)))
  
  override def getByLocation(location: Location, radius: Long): Future[List[Person]] = Future.successful {
    list.values.toList.filter(person => calculateDistance(location, person.location) < radius)
  }
  
  override def getByTime(start: Long, optionEnd: Option[Long]): Future[List[Person]] = Future.successful {
    optionEnd.map(end => list.values.filter(person => person.time >= start && person.time <= end))
    .getOrElse(list.values.filter(person => person.time >= start && person.time <= (start + time)))
    .toList
  }
  
  override def getByLocationAndTime(location: Location, radius: Long, start: Long, optionEnd: Option[Long]): Future[List[Person]] = Future.successful {
    val filterdList = list.values.toList.filter(person => calculateDistance(location, person.location) <= radius)
    optionEnd.map(end => filterdList.filter(person => person.time >= start && person.time <= end))
    .getOrElse(filterdList.filter(person => person.time >= start && person.time <= (start + time)))
    .toList
  }
  
  override def save(person: Person): Future[Person] = {
    val id = person.id.getOrElse((list.size + 1).toString)
    val toSave = person.copy(id = Option(id))
    list += id -> toSave
    Future.successful(toSave)
  }
  
  def calculateDistance(location1: Location, location2: Location): Double = {
    val R = 6371    
    val dLat = (location1.latitude - location2.latitude).toRadians
    val dLon = (location1.longitude - location2.longitude).toRadians
    val a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(location2.latitude.toRadians) * Math.cos(location1.latitude.toRadians)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    Math.asin(Math.min(1, Math.sqrt(a))) * (6371 * 2)
  }
}