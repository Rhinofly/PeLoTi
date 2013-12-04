package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import models.Person
import service.Service
import play.api.libs.json._
import play.api.libs.functional.syntax._

class Application(service: Service) extends Controller {

  implicit val reads = (
    (__ \ 'latitude).read[Double] and
    (__ \ 'longitude).read[Double]) tupled

  def search = Action(parse.json) { request =>

    request.body.validate[(Double, Double)].map {
      case (latitude, longitude) =>
        Ok(Json.obj("status" -> "OK", "people" -> service.searchPeople(latitude, longitude)))
    }.recoverTotal {
      e => BadRequest(JsError.toFlatJson(e))
    }
  }

  def create = Action(parse.json) { request =>
    request.body.validate[(Double, Double)].map {
      case (latitude, longitude) =>
        Ok(Json.obj("status" -> "OK", "id" -> service.createPerson(latitude, longitude)))
    }.recoverTotal {
      e => BadRequest(JsError.toFlatJson(e))
    }
  }
}

object Application extends Application(new Service)
