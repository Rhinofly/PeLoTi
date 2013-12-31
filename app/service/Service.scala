package service

import models._
import play.api.libs.Codecs
import scala.collection.immutable.StringOps
import play.api.libs.json._
import models.repository._
import play.api.http.Status._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.util._
import play.api.libs.json.Json.JsValueWrapper
import models.repository.PersonRepository
import models.UpdateRequest

class Service(repository: PersonRepository) {
  
  def getByLocation(longitude: Double, latitude: Double, radius: Long = 10): Future[JsObject] = {
    repository.getByLocation(Location(longitude, latitude), radius).map {
      list => createResponse("people" -> list)
    }
  }
  
  def savePerson(request: UpdateRequest): Future[JsObject] = {
    repository.save(Person(Location(request.latitude, request.longitude), request.time, request.id)).map(id => 
     createResponse("id" -> id))
  }
  
  def getById(id: String): Future[JsObject] = {
    try {
      repository.getById(id).map(person => 
        createResponse("location" -> person.location))
    } catch {
      case e: Exception => Future(Json.obj("status" -> BAD_REQUEST, "message" -> e.getMessage))
    }
  }
  
  def getByTime(start: Long, end: Option[Long]): Future[JsObject] = {
    repository.getByTime(start, end).map(list => createResponse("people" -> list))
  }
  
  def getByLocationAndTime(longitude: Double, latitude: Double, start: Long, end: Option[Long]): Future[JsObject] = {
    repository.getByLocationAndTime(Location(longitude, latitude), 10, start, end).map(list => createResponse("people" -> list))
  }
  
  def createResponse(response: (String, JsValueWrapper)): JsObject = {
    Json.obj("status" -> OK, response)
  }
}

object Service {
  def apply(repository: PersonRepository): Service = {
    new Service(repository)
  }
}