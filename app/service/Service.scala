package service

import models._
import play.api.libs.Codecs
import scala.collection.immutable.StringOps
import play.api.libs.json._
import models.requests._
import play.api.http.Status._
import models.requests._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util._

class Service(repository: PersonRepository) {
  
  def getByLocation(longitude: Double, latitude: Double, radius: Long = 10): Future[JsObject] = {
    repository.getByLocation(Location(longitude, latitude), radius).map {
      list => Json.obj("status" -> OK, "people" -> list)
    }
  }
  
  def savePerson(request: UpdateRequest): Future[JsObject] = {
    repository.save(Person(Location(request.latitude, request.longitude), request.time, request.id)).map(id => 
      Json.obj("status" -> OK, "id" -> id))
  }
  
  def getById(id: String): Future[JsObject] = {
    try {
      repository.getById(id).map(person => 
        Json.obj("status" -> OK, "location" -> person.location))
    } catch {
      case e: Exception => Future(Json.obj("status" -> BAD_REQUEST, "message" -> e.getMessage))
    }
  }
  
  def getByTime(time: Long): Future[JsObject] = {
    repository.getByTime(time).map(list => Json.obj("status" -> OK, "people" -> list))
  }
  
  def getByLocationAndTime(longitude: Double, latitude: Double, time: Long): Future[JsObject] = {
    repository.getByLocationAndTime(Location(longitude, latitude), 10, time).map(list => Json.obj("status" -> OK, "people" -> list))
  }
}

object Service {
  def apply(repository: PersonRepository): Service = {
    new Service(repository)
  }
}