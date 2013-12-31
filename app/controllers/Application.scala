package controllers

import scala.concurrent.Future
import models._
import models.repository._
import play.api.libs.json.JsError
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import service.Service
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import scala.util._
import play.api.data._
import play.api.data.Form._
import play.api.data.Forms._
import play.api.data.format.Formats._
import models.UpdateRequest

class Application(service: Service) extends Controller {

  def byLocation(longitude: Double, latitude: Double, radius: Long) = Action.async {
    service.getByLocation(longitude, latitude, radius).map(list => Ok(list))
  }

  def save = Action.async { implicit request =>
    saveForm.bindFromRequest.fold(
      formWithErrors => { Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid parameters"))) },
      data => {
        service.savePerson(data).map(json => Ok(json))
      })
  }

  def byId(id: String) = Action.async {
      service.getById(id).map(person => Ok(person))
  }
  
  def getByTime(start: Long, end: Option[Long]) = Action.async {
      service.getByTime(start, end).map(list => Ok(list))
  }
  
  def getByLocationAndTime(longitude: Double, latitude: Double, start: Long, end: Option[Long]) = Action.async {
      service.getByLocationAndTime(longitude, latitude, start, end).map(list => Ok(list))
  }

  def saveForm = Form(
    mapping(
      "longitude" -> of[Double],
      "latitude" -> of[Double],
      "time" -> of[Long],
      "id" -> optional(text))(UpdateRequest.apply)(UpdateRequest.unapply))
}

object Application extends Application(Service(new MongoPersonRepository(Config.databaseName, "test")))