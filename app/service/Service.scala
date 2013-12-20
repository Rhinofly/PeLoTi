package service

import models.Person
import models.Database
import play.api.libs.Codecs
import scala.collection.immutable.StringOps
import play.api.libs.json._
import models.requests.RequestHandler
import play.api.http.Status._
import models.requests._

class Service(database: Database) {
  
  def searchPeople(request: MainRequest): JsObject = {
    val result = database.search(request.latitude, request.longitude, 10, request.token)
    Json.obj("status" -> OK, "people" -> result)
  }
  
  def createPerson(request: MainRequest): JsObject = {
    val id = database.create(request.latitude, request.longitude, request.token)
    Json.obj("status" -> OK, "id" -> id)
  }
  
  def updatePerson(request: UpdateRequest): JsObject = {
    val status = database.update(request.id, request.latitude, request.longitude, request.token)
    Json.obj("status" -> status)
  }
  
  def getPerson(request: GetRequest): JsObject = {
    try {
      val person = database.getPerson(request.id, request.token)
      Json.obj("status" -> OK, "latitude" -> person.location(0), "longitude" -> person.location(1))
    } catch {
      case e: Exception => Json.obj("status" -> BAD_REQUEST, "message" -> e.getMessage) 
    }
  }
  
  def generateToken(email: String): String = {
    Codecs.md5(email.getBytes)
  }
}

object Service {
  def apply(database: Database): Service = {
    new Service(database)
  }
}