package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import models.Person
import service.Service
import play.api.libs.json._
import play.api.libs.functional.syntax._

class Application(service: Service) extends Controller {
  
  def search = Action(parse.json) { request =>
    val latitude = (request.body \ "latitude").as[Double]
    val longitude = (request.body \ "longitude").as[Double]
    Ok(Json.obj("status" -> "OK", "people" -> service.searchPeople(latitude, longitude)))
  }
}

object Application extends Application(new Service)
