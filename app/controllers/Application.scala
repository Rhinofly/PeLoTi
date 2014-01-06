package controllers

import scala.concurrent.Future

import models._
import models.repository.MongoPersonRepository
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc._
import service.Service

class Application(service: Service) extends Controller {

  def byLocation(longitude: Double, latitude: Double, radius: Long) = Action.async {
    service.getByLocation(longitude, latitude, radius).map(list => Ok(list))
  }

  def save = Action.async { implicit request =>
    saveForm.bindFromRequest.fold(
      formWithErrors => formErrors(formWithErrors),
      data => service.savePerson(data).map(json => Ok(json)))
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

  def storeExtra = Action.async { implicit request =>
    extraDataForm.bindFromRequest.fold(
      formWithErrors => formErrors(formWithErrors),
      data => service.storeData(data).map(json => Ok(json)))
  }

  def formErrors[T](form: Form[T]) = {
    Future(BadRequest(Json.obj("status" -> BAD_REQUEST, "message" -> form.globalErrors.map(error => error.message).mkString(", "))))
  }

  def saveForm = Form(
    mapping(
      "longitude" -> of[Double],
      "latitude" -> of[Double],
      "time" -> of[Long],
      "id" -> optional(text))(UpdateRequest.apply)(UpdateRequest.unapply))

  def extraDataForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "extras" -> list(
        mapping(
          "key" -> nonEmptyText,
          "value" -> nonEmptyText)(ExtraField)(ExtraField.unapply)))(ExtraDataRequest)(ExtraDataRequest.unapply))
}


object Application extends Application(Service(new MongoPersonRepository(Config.databaseName, "test")))