package service

import models.Person
import models.Database
import play.api.libs.Codecs
import scala.collection.immutable.StringOps
import play.api.libs.json._
import models.requests.PlainRequest
import models.requests.UpdateRequest
import play.api.http.Status.OK

class Service(database: Database) {
  
  def searchPeople(request: PlainRequest): JsObject = {
    val result = database.search(request.latitude, request.longitude, 10, request.token)
    Json.obj("status" -> OK, "people" -> result)
  }
  
  def createPerson(request: PlainRequest): JsObject = {
    val id = database.create(request.latitude, request.longitude, request.token)
    Json.obj("status" -> OK, "id" -> id)
  }
  
  def updatePerson(request: UpdateRequest): JsObject = {
    val status = database.update(request.id, request.latitude, request.longitude, request.token)
    Json.obj("status" -> status)
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