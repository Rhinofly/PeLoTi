package controllers

import scala.concurrent.Future
import models._
import models.requests._
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

class Application(service: Service) extends Controller {

  def byLocation(longitude: Option[Double], latitude: Option[Double]) = Action.async {
    if (longitude.isDefined && latitude.isDefined)
      service.getByLocation(longitude.get, latitude.get).map(list => Ok(list))
    else
      Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid parameters")))
  }

  def save = Action.async { implicit request =>
    saveForm.bindFromRequest.fold(
      formWithErrors => { Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid parameters"))) },
      data => {
        service.savePerson(data).map(json => Ok(json))
      })
  }

  def byId(id: Option[String]) = Action.async {
    if (id.isDefined)
      service.getById(id.get).map(person => Ok(person))
    else
      Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid parameters")))
  }
  
  def getByTime(time: Option[Long]) = Action.async {
    if(time.isDefined)
      service.getByTime(time.get).map(list => Ok(list))
    else
      Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid parameters")))
  }
  
  def getByLocationAndTime(longitude: Option[Double], latitude: Option[Double], time: Option[Long]) = Action.async {
    if(longitude.isDefined && latitude.isDefined && time.isDefined)
      service.getByLocationAndTime(longitude.get, latitude.get, time.get).map(list => Ok(list))
    else
      Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> "Invalid parameters")))
  }

  def saveForm = Form(
    mapping(
      "longitude" -> of[Double],
      "latitude" -> of[Double],
      "time" -> of[Long],
      "id" -> optional(text))(UpdateRequest.apply)(UpdateRequest.unapply))
}

object Application extends Application(Service(new MongoDB(Config.databaseName, "test")))