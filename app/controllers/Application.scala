package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import models.Person
import service.Service

class Application extends Controller {
  
  def search = Action(parse.json) { request =>
    val latitude = (request.body \ "latitude").as[Double]
    val longitude = (request.body \ "longitude").as[Double]
    Ok(Json.obj("status" -> "OK", "people" -> Service.searchPeople(latitude, longitude)))
  }
}

object Application extends Application