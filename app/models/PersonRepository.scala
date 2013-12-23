package models

import com.mongodb.casbah.Imports._
import scala.concurrent.Future

trait PersonRepository {
  
  def getById(id: String): Future[Person]
  def getByLocation(longitude: Double, latitude: Double, radius: Long): Future[List[Person]]
  def save(longitude: Double, latitude: Double, id: Option[String]): Future[String]
}